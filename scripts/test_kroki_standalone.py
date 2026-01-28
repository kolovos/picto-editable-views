#!/usr/bin/env python3
"""
Standalone test script to verify Kroki API calls work correctly.
No external dependencies required beyond 'requests'.

Run with: python test_kroki_standalone.py
"""

import base64
import sys
import zlib

import requests

KROKI_URL = "https://kroki.io"


def encode_kroki(source: str) -> str:
    """Encode source for Kroki URL using deflate + base64url."""
    compressed = zlib.compress(source.encode('utf-8'), 9)
    return base64.urlsafe_b64encode(compressed).decode('ascii')


def fetch_diagram(diagram_type: str, source: str, output_format: str = 'svg'):
    """Fetch a diagram from Kroki.io."""
    encoded = encode_kroki(source)
    url = f"{KROKI_URL}/{diagram_type}/{output_format}/{encoded}"

    response = requests.get(url, timeout=30)
    if response.status_code == 200:
        content = response.content
        if not content:
            return b'', "Empty response from server"
        return content, None
    else:
        return b'', f"HTTP {response.status_code}: {response.text[:200]}"


def inject_zero_width_chars(source: str, element: str, char: str, count: int) -> tuple[str, bool]:
    """Inject zero-width characters after the first occurrence of element name."""
    if element not in source:
        return source, False
    chars_to_inject = char * count
    return source.replace(element, element + chars_to_inject, 1), True


def test_encoding():
    """Test that encoding/decoding round-trips correctly."""
    print("=" * 60)
    print("TEST: Encoding round-trip")
    print("=" * 60)

    test_cases = [
        "digraph G { Hello -> World }",
        "digraph G { Hello\u2060 -> World }",  # With zero-width char
        "@startuml\nAlice -> Bob: Hello\n@enduml",
        "graph TD\n    A --> B",
    ]

    all_passed = True
    for source in test_cases:
        encoded = encode_kroki(source)
        decoded = zlib.decompress(base64.urlsafe_b64decode(encoded)).decode('utf-8')
        passed = source == decoded
        status = "PASS" if passed else "FAIL"
        print(f"  [{status}] {repr(source[:40])}...")
        if not passed:
            print(f"         Expected: {repr(source)}")
            print(f"         Got:      {repr(decoded)}")
            all_passed = False

    return all_passed


def test_fetch_basic():
    """Test basic diagram fetching."""
    print()
    print("=" * 60)
    print("TEST: Basic diagram fetching")
    print("=" * 60)

    test_cases = [
        ('graphviz', 'digraph G { A -> B }'),
        ('plantuml', '@startuml\nA -> B\n@enduml'),
        ('mermaid', 'graph TD\n    A --> B'),
        ('d2', 'A -> B'),
    ]

    all_passed = True
    for diagram_type, source in test_cases:
        content, error = fetch_diagram(diagram_type, source)
        passed = error is None and len(content) > 0
        status = "PASS" if passed else "FAIL"
        print(f"  [{status}] {diagram_type}: {len(content)} bytes")
        if error:
            print(f"         Error: {error}")
            all_passed = False

    return all_passed


def test_zero_width_injection():
    """Test zero-width character injection."""
    print()
    print("=" * 60)
    print("TEST: Zero-width character injection")
    print("=" * 60)

    source = "digraph G { Hello -> World }"
    test_cases = [
        ('\u2060', 'Word Joiner', 1),
        ('\u2060', 'Word Joiner', 10),
        ('\u2060', 'Word Joiner', 100),
        ('\u2061', 'Function Application', 10),
        ('\u2064', 'Invisible Plus', 10),
    ]

    all_passed = True
    for char, name, count in test_cases:
        modified, ok = inject_zero_width_chars(source, 'Hello', char, count)

        if not ok:
            print(f"  [FAIL] {name} x{count}: element not found in source")
            all_passed = False
            continue

        # Verify injection worked
        expected_len = len(source) + count
        if len(modified) != expected_len:
            print(f"  [FAIL] {name} x{count}: wrong length {len(modified)} != {expected_len}")
            all_passed = False
            continue

        # Verify Kroki accepts it
        content, error = fetch_diagram('graphviz', modified)
        passed = error is None and len(content) > 0
        status = "PASS" if passed else "FAIL"
        print(f"  [{status}] {name} x{count}: {len(content)} bytes")
        if error:
            print(f"         Error: {error}")
            all_passed = False

    # Test element not found case
    print()
    print("  Testing element-not-found case:")
    modified, ok = inject_zero_width_chars(source, 'NotFound', '\u2060', 1)
    if not ok and modified == source:
        print(f"  [PASS] Element not found returns (source, False)")
    else:
        print(f"  [FAIL] Element not found should return (source, False)")
        all_passed = False

    return all_passed


def test_svg_content():
    """Test that SVG content is valid."""
    print()
    print("=" * 60)
    print("TEST: SVG content validation")
    print("=" * 60)

    source = "digraph G { Hello -> World }"
    content, error = fetch_diagram('graphviz', source)

    if error:
        print(f"  [FAIL] Fetch failed: {error}")
        return False

    # Check for SVG markers
    has_svg_tag = b'<svg' in content.lower()
    has_xml_decl = b'<?xml' in content
    is_svg = has_svg_tag or has_xml_decl

    print(f"  Content length: {len(content)} bytes")
    print(f"  Has <svg> tag: {has_svg_tag}")
    print(f"  Has XML declaration: {has_xml_decl}")
    print(f"  [{('PASS' if is_svg else 'FAIL')}] Valid SVG content")

    # Show first 200 chars for debugging
    print()
    print("  First 200 bytes of content:")
    print(f"  {content[:200]}")

    return is_svg


def test_zero_width_svg_comparison():
    """Compare SVG output with and without zero-width chars."""
    print()
    print("=" * 60)
    print("TEST: SVG comparison (baseline vs zero-width)")
    print("=" * 60)

    source = "digraph G { Hello -> World }"

    # Get baseline
    baseline, error = fetch_diagram('graphviz', source)
    if error:
        print(f"  [FAIL] Baseline fetch failed: {error}")
        return False

    print(f"  Baseline: {len(baseline)} bytes")

    # Get with zero-width chars
    modified, ok = inject_zero_width_chars(source, 'Hello', '\u2060', 10)
    if not ok:
        print(f"  [FAIL] Could not inject ZWC")
        return False
    modified_svg, error = fetch_diagram('graphviz', modified)
    if error:
        print(f"  [FAIL] Modified fetch failed: {error}")
        return False

    print(f"  Modified: {len(modified_svg)} bytes")

    # Compare
    same_length = len(baseline) == len(modified_svg)
    same_content = baseline == modified_svg

    print(f"  Same length: {same_length}")
    print(f"  Same content: {same_content}")

    # If different, show where they differ
    if not same_content:
        print()
        print("  Difference analysis:")
        # Find first difference
        min_len = min(len(baseline), len(modified_svg))
        for i in range(min_len):
            if baseline[i] != modified_svg[i]:
                print(f"    First difference at byte {i}")
                print(f"    Baseline: ...{baseline[max(0,i-20):i+20]}...")
                print(f"    Modified: ...{modified_svg[max(0,i-20):i+20]}...")
                break

    return True  # This test is informational


def main():
    print("Kroki API Test Suite")
    print("=" * 60)
    print()

    results = []

    results.append(("Encoding round-trip", test_encoding()))
    results.append(("Basic fetching", test_fetch_basic()))
    results.append(("Zero-width injection", test_zero_width_injection()))
    results.append(("SVG content validation", test_svg_content()))
    results.append(("SVG comparison", test_zero_width_svg_comparison()))

    # Summary
    print()
    print("=" * 60)
    print("SUMMARY")
    print("=" * 60)

    all_passed = True
    for name, passed in results:
        status = "PASS" if passed else "FAIL"
        print(f"  [{status}] {name}")
        if not passed:
            all_passed = False

    print()
    if all_passed:
        print("All tests passed!")
        return 0
    else:
        print("Some tests failed!")
        return 1


if __name__ == '__main__':
    sys.exit(main())
