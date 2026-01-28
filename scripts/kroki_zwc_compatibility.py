#!/usr/bin/env python3
"""
Test script to verify zero-width character compatibility with diagram-as-code
tools via Kroki.io API.

This tests whether zero-width characters (used for traceability in Picto)
cause issues when rendering diagrams.
"""

import base64
import csv
import io
import json
import sys
import time
import zlib
from datetime import datetime
from pathlib import Path
from typing import Optional

import cairosvg
import requests
from PIL import Image

from diagrams import DIAGRAMS

# Configuration
KROKI_URL = "https://kroki.io"
REQUEST_DELAY = 0.5  # Delay between requests to respect rate limits
OUTPUT_DIR = Path("test_output")  # Directory to save PNG files

# Zero-width characters to test
ZERO_WIDTH_CHARS = {
    '\u2060': 'Word Joiner (U+2060)',
    '\u2061': 'Function Application (U+2061)',
    '\u2064': 'Invisible Plus (U+2064)',
}

# Character counts to test
CHAR_COUNTS = [1, 10, 100, 1000]


def encode_kroki(source: str) -> str:
    """Encode source for Kroki URL using deflate + base64url."""
    compressed = zlib.compress(source.encode('utf-8'), 9)
    return base64.urlsafe_b64encode(compressed).decode('ascii')


def fetch_diagram(diagram_type: str, source: str, output_format: str = 'svg') -> tuple[bytes, Optional[str]]:
    """
    Fetch a diagram from Kroki.io.

    Returns:
        Tuple of (content_bytes, error_message)
        If successful, error_message is None.
    """
    encoded = encode_kroki(source)
    url = f"{KROKI_URL}/{diagram_type}/{output_format}/{encoded}"

    try:
        response = requests.get(url, timeout=30)
        if response.status_code == 200:
            content = response.content
            if not content:
                return b'', "Empty response from server"
            return content, None
        else:
            return b'', f"HTTP {response.status_code}: {response.text[:200]}"
    except requests.RequestException as e:
        return b'', f"Request failed: {str(e)}"


def svg_to_png(svg_content: bytes) -> Optional[Image.Image]:
    """Convert SVG content to PNG Image for comparison."""
    try:
        png_data = cairosvg.svg2png(bytestring=svg_content)
        return Image.open(io.BytesIO(png_data))
    except Exception as e:
        print(f"  Warning: SVG to PNG conversion failed: {e}")
        return None


def save_png(img: Image.Image, filepath: Path) -> None:
    """Save PIL Image to PNG file."""
    filepath.parent.mkdir(parents=True, exist_ok=True)
    img.save(filepath, 'PNG')


def save_svg(svg_content: bytes, filepath: Path) -> None:
    """Save SVG content to file."""
    filepath.parent.mkdir(parents=True, exist_ok=True)
    with open(filepath, 'wb') as f:
        f.write(svg_content)


def compare_images(img1: Image.Image, img2: Image.Image) -> float:
    """
    Compare two images and return similarity percentage.

    Returns:
        Float between 0.0 and 1.0 representing similarity.
    """
    # Convert to same mode
    img1 = img1.convert('RGBA')
    img2 = img2.convert('RGBA')

    # Handle size differences
    if img1.size != img2.size:
        # Images are different sizes - this might indicate a problem
        # but could also be due to text rendering differences
        max_width = max(img1.width, img2.width)
        max_height = max(img1.height, img2.height)

        # Create new images with same size (white background)
        new_img1 = Image.new('RGBA', (max_width, max_height), (255, 255, 255, 255))
        new_img2 = Image.new('RGBA', (max_width, max_height), (255, 255, 255, 255))

        new_img1.paste(img1, (0, 0))
        new_img2.paste(img2, (0, 0))

        img1 = new_img1
        img2 = new_img2

    # Compare pixels using raw bytes (4 bytes per pixel for RGBA)
    bytes1 = img1.tobytes()
    bytes2 = img2.tobytes()

    if len(bytes1) != len(bytes2):
        return 0.0

    # Compare byte by byte, count matching bytes
    # Since RGBA has 4 bytes per pixel, we compare at pixel level
    total_pixels = len(bytes1) // 4
    matching_pixels = 0

    for i in range(0, len(bytes1), 4):
        if bytes1[i:i+4] == bytes2[i:i+4]:
            matching_pixels += 1

    return matching_pixels / total_pixels


def inject_zero_width_chars(source: str, element: str, char: str, count: int) -> tuple[str, bool]:
    """
    Inject zero-width characters after the first occurrence of element name.

    The characters are appended directly after the element name.

    Returns:
        Tuple of (modified_source, success).
        If element is not found in source, returns (source, False).
    """
    if element not in source:
        return source, False
    chars_to_inject = char * count
    # Replace only the first occurrence
    return source.replace(element, element + chars_to_inject, 1), True


def sanitize_filename(name: str) -> str:
    """Sanitize a string for use as a filename."""
    # Replace problematic characters
    return name.replace('\\', '_').replace('/', '_').replace(':', '_')


def run_test(diagram_type: str, subtype: str, source: str, element: str, output_dir: Path) -> list[dict]:
    """
    Run all zero-width character tests for a specific diagram.

    Returns:
        List of test result dictionaries.
    """
    results = []
    full_type = f"{diagram_type}/{subtype}" if subtype != 'default' else diagram_type
    safe_full_type = sanitize_filename(full_type)

    print(f"\nTesting: {full_type}")

    # Create output directory for this diagram type
    diagram_output_dir = output_dir / safe_full_type
    diagram_output_dir.mkdir(parents=True, exist_ok=True)

    # Fetch baseline
    print("  Fetching baseline...", end=" ", flush=True)
    baseline_svg, baseline_error = fetch_diagram(diagram_type, source)

    if baseline_error:
        print(f"ERROR: {baseline_error}")
        # Return one error result per ZWC/count combination so CSV has complete data
        error_results = []
        for char, char_name in ZERO_WIDTH_CHARS.items():
            for count in CHAR_COUNTS:
                char_repr = repr(char)[1:-1]
                error_results.append({
                    'diagram_type': full_type,
                    'zero_width_char': char_repr,
                    'char_name': char_name,
                    'char_count': count,
                    'success': False,
                    'error': f"Baseline failed: {baseline_error}",
                    'similarity': None,
                    'baseline_size': None,
                    'result_size': None,
                    'is_baseline_error': True,
                })
        return error_results

    # Save baseline SVG
    save_svg(baseline_svg, diagram_output_dir / "baseline.svg")

    baseline_img = svg_to_png(baseline_svg)
    if baseline_img is None:
        print("ERROR: Could not convert baseline SVG to PNG")
        # Return one error result per ZWC/count combination so CSV has complete data
        error_results = []
        for char, char_name in ZERO_WIDTH_CHARS.items():
            for count in CHAR_COUNTS:
                char_repr = repr(char)[1:-1]
                error_results.append({
                    'diagram_type': full_type,
                    'zero_width_char': char_repr,
                    'char_name': char_name,
                    'char_count': count,
                    'success': False,
                    'error': 'Baseline SVG to PNG conversion failed',
                    'similarity': None,
                    'baseline_size': None,
                    'result_size': None,
                    'is_baseline_error': True,
                })
        return error_results

    # Save baseline PNG
    save_png(baseline_img, diagram_output_dir / "baseline.png")

    print(f"OK ({baseline_img.width}x{baseline_img.height})")
    time.sleep(REQUEST_DELAY)

    # Test each zero-width character and count
    for char, char_name in ZERO_WIDTH_CHARS.items():
        for count in CHAR_COUNTS:
            char_repr = repr(char)[1:-1]  # Get \uXXXX representation
            char_code = char_repr.replace('\\u', 'U')  # U2060 format for filenames
            test_label = f"  {char_repr} x{count}:"
            print(f"{test_label:<20}", end=" ", flush=True)

            # Initialize result dict early so it's available for all code paths
            result = {
                'diagram_type': full_type,
                'zero_width_char': char_repr,
                'char_name': char_name,
                'char_count': count,
                'baseline_size': [baseline_img.width, baseline_img.height],
            }

            modified_source, injection_ok = inject_zero_width_chars(source, element, char, count)
            if not injection_ok:
                print(f"ERROR: Element '{element}' not found in source")
                result.update({
                    'success': False,
                    'error': f"Element '{element}' not found in source",
                    'similarity': None,
                    'result_size': None,
                    'png_file': None,
                    'svg_file': None,
                })
                results.append(result)
                continue

            test_svg, test_error = fetch_diagram(diagram_type, modified_source)

            # Filename for this test
            test_filename = f"{char_code}_x{count}"

            if test_error:
                print(f"ERROR: {test_error}")
                result.update({
                    'success': False,
                    'error': test_error,
                    'similarity': None,
                    'result_size': None,
                    'png_file': None,
                    'svg_file': None,
                })
            elif not test_svg:
                print(f"ERROR: Empty SVG content received")
                result.update({
                    'success': False,
                    'error': 'Empty SVG content',
                    'similarity': None,
                    'result_size': None,
                    'png_file': None,
                    'svg_file': None,
                })
            else:
                # Save SVG
                svg_path = diagram_output_dir / f"{test_filename}.svg"
                save_svg(test_svg, svg_path)
                result['svg_file'] = str(svg_path)

                test_img = svg_to_png(test_svg)
                if test_img is None:
                    print("ERROR: SVG to PNG conversion failed")
                    result.update({
                        'success': False,
                        'error': 'SVG to PNG conversion failed',
                        'similarity': None,
                        'result_size': None,
                        'png_file': None,
                    })
                else:
                    # Save PNG
                    png_path = diagram_output_dir / f"{test_filename}.png"
                    save_png(test_img, png_path)
                    result['png_file'] = str(png_path)

                    similarity = compare_images(baseline_img, test_img)
                    # Consider >95% similarity as passing (allows for minor rendering differences)
                    passed = similarity > 0.95
                    status = "OK" if passed else "DIFF"
                    print(f"{status}, {similarity*100:.2f}% similar ({test_img.width}x{test_img.height})")

                    result.update({
                        'success': True,
                        'error': None,
                        'similarity': similarity,
                        'result_size': [test_img.width, test_img.height],
                        'visual_match': passed,
                    })

            results.append(result)
            time.sleep(REQUEST_DELAY)

    return results


def save_csv_results(results: list[dict], filepath: Path) -> None:
    """
    Save test results to CSV file.

    Columns: tool, diagram_type, zwc_type, zwc_count, test_ran, similarity_percent
    """
    filepath.parent.mkdir(parents=True, exist_ok=True)

    with open(filepath, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)

        # Write header
        writer.writerow([
            'tool',
            'diagram_type',
            'zwc_type',
            'zwc_count',
            'test_ran',
            'similarity_percent'
        ])

        # Write data rows
        for r in results:
            # Parse tool and diagram_type from the combined diagram_type field
            full_type = r.get('diagram_type', '')
            if '/' in full_type:
                tool, diagram_subtype = full_type.split('/', 1)
            else:
                tool = full_type
                diagram_subtype = 'default'

            zwc_type = r.get('char_name', r.get('zero_width_char', ''))
            zwc_count = r.get('char_count', 0)

            # Test ran successfully if no error
            test_ran = 'yes' if r.get('error') is None else 'no'

            # Similarity percentage (empty if test didn't run)
            similarity = r.get('similarity')
            if similarity is not None:
                similarity_percent = f"{similarity * 100:.2f}"
            else:
                similarity_percent = ''

            writer.writerow([
                tool,
                diagram_subtype,
                zwc_type,
                zwc_count,
                test_ran,
                similarity_percent
            ])


def main():
    """Main entry point."""
    print("Testing zero-width character compatibility with Kroki.io")
    print("=" * 60)
    print(f"Zero-width characters: {', '.join(repr(c)[1:-1] for c in ZERO_WIDTH_CHARS.keys())}")
    print(f"Character counts: {CHAR_COUNTS}")
    print(f"Diagram types: {len(DIAGRAMS)}")

    # Count total subtypes
    total_diagrams = sum(len(subtypes) for subtypes in DIAGRAMS.values())
    print(f"Total diagram variants: {total_diagrams}")

    # Setup output directory
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_dir = OUTPUT_DIR / timestamp
    output_dir.mkdir(parents=True, exist_ok=True)
    print(f"Output directory: {output_dir}")
    print()

    all_results = []

    for diagram_type, subtypes in DIAGRAMS.items():
        for subtype, (source, element) in subtypes.items():
            results = run_test(diagram_type, subtype, source, element, output_dir)
            all_results.extend(results)

    # Calculate summary
    total = len(all_results)
    errors = sum(1 for r in all_results if r.get('error') is not None)
    successful = [r for r in all_results if r.get('error') is None]
    visual_matches = sum(1 for r in successful if r.get('visual_match', False))
    visual_diffs = len(successful) - visual_matches

    # Print summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    print(f"  Total tests:     {total}")
    print(f"  Rendered OK:     {len(successful)}")
    print(f"  Visual matches:  {visual_matches}")
    print(f"  Visual diffs:    {visual_diffs}")
    print(f"  Errors:          {errors}")

    # List any issues
    if errors > 0 or visual_diffs > 0:
        print("\nIssues found:")
        for r in all_results:
            if r.get('error'):
                print(f"  ERROR: {r['diagram_type']} {r['zero_width_char']} x{r['char_count']}: {r['error']}")
            elif not r.get('visual_match', True):
                png_file = r.get('png_file', 'N/A')
                print(f"  DIFF:  {r['diagram_type']} {r['zero_width_char']} x{r['char_count']}: {r['similarity']*100:.2f}% similar")
                print(f"         PNG: {png_file}")

    # Save results to JSON
    output = {
        'timestamp': datetime.now().isoformat(),
        'output_directory': str(output_dir),
        'config': {
            'zero_width_chars': {repr(k)[1:-1]: v for k, v in ZERO_WIDTH_CHARS.items()},
            'char_counts': CHAR_COUNTS,
            'kroki_url': KROKI_URL,
            'diagram_types': list(DIAGRAMS.keys()),
        },
        'results': all_results,
        'summary': {
            'total': total,
            'rendered_ok': len(successful),
            'visual_matches': visual_matches,
            'visual_diffs': visual_diffs,
            'errors': errors,
        }
    }

    # Save to both timestamped dir and current dir
    output_file = output_dir / 'test_results.json'
    with open(output_file, 'w') as f:
        json.dump(output, f, indent=2)
    print(f"\nDetailed results saved to: {output_file}")

    # Also save to current directory for convenience
    with open('test_results.json', 'w') as f:
        json.dump(output, f, indent=2)
    print(f"Also saved to: test_results.json")

    # Save CSV results
    csv_file = output_dir / 'test_results.csv'
    save_csv_results(all_results, csv_file)
    print(f"CSV results saved to: {csv_file}")

    # Also save CSV to current directory
    save_csv_results(all_results, Path('test_results.csv'))
    print(f"Also saved to: test_results.csv")

    print(f"\nAll PNG/SVG files saved to: {output_dir}")

    # Exit with non-zero code if there were issues
    if errors > 0 or visual_diffs > 0:
        sys.exit(1)

    print("\nAll tests passed!")
    sys.exit(0)


if __name__ == '__main__':
    main()
