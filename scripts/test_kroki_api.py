#!/usr/bin/env python3
"""
Test cases to verify Kroki API calls are correct.

Run with: pytest test_kroki_api.py -v
"""

import base64
import zlib

import pytest
import requests

from kroki_zwc_compatibility import encode_kroki, fetch_diagram, inject_zero_width_chars

KROKI_URL = "https://kroki.io"


class TestEncodeKroki:
    """Test the Kroki encoding function."""

    def test_encode_simple_graphviz(self):
        """Test encoding matches Kroki's expected format."""
        source = "digraph G { Hello -> World }"
        encoded = encode_kroki(source)

        # Verify it's valid base64url
        assert encoded == base64.urlsafe_b64encode(
            zlib.compress(source.encode('utf-8'), 9)
        ).decode('ascii')

        # Verify we can decode it back
        decoded = zlib.decompress(base64.urlsafe_b64decode(encoded))
        assert decoded.decode('utf-8') == source

    def test_encode_with_unicode(self):
        """Test encoding handles unicode characters correctly."""
        source = "digraph G { Hello -> Wörld }"
        encoded = encode_kroki(source)

        decoded = zlib.decompress(base64.urlsafe_b64decode(encoded))
        assert decoded.decode('utf-8') == source

    def test_encode_with_zero_width_chars(self):
        """Test encoding handles zero-width characters correctly."""
        source = "digraph G { Hello\u2060 -> World }"
        encoded = encode_kroki(source)

        decoded = zlib.decompress(base64.urlsafe_b64decode(encoded))
        assert decoded.decode('utf-8') == source
        assert '\u2060' in decoded.decode('utf-8')

    def test_encode_multiline(self):
        """Test encoding handles multiline content."""
        source = """digraph G {
    Hello -> World
    World -> Hello
}"""
        encoded = encode_kroki(source)

        decoded = zlib.decompress(base64.urlsafe_b64decode(encoded))
        assert decoded.decode('utf-8') == source


class TestFetchDiagram:
    """Test fetching diagrams from Kroki API."""

    def test_fetch_simple_graphviz(self):
        """Test fetching a simple GraphViz diagram."""
        source = "digraph G { Hello -> World }"
        content, error = fetch_diagram('graphviz', source)

        assert error is None, f"Unexpected error: {error}"
        assert content, "Content should not be empty"
        assert b'<svg' in content.lower() or b'<?xml' in content, "Should return SVG content"

    def test_fetch_graphviz_png(self):
        """Test fetching a diagram as PNG."""
        source = "digraph G { Hello -> World }"
        content, error = fetch_diagram('graphviz', source, output_format='png')

        assert error is None, f"Unexpected error: {error}"
        assert content, "Content should not be empty"
        # PNG magic bytes
        assert content[:8] == b'\x89PNG\r\n\x1a\n', "Should return PNG content"

    def test_fetch_plantuml_sequence(self):
        """Test fetching a PlantUML sequence diagram."""
        source = """@startuml
Alice -> Bob: Hello
Bob --> Alice: Hi
@enduml"""
        content, error = fetch_diagram('plantuml', source)

        assert error is None, f"Unexpected error: {error}"
        assert content, "Content should not be empty"
        assert b'<svg' in content.lower() or b'<?xml' in content

    def test_fetch_mermaid(self):
        """Test fetching a Mermaid diagram."""
        source = """sequenceDiagram
    Alice->>John: Hello John
    John-->>Alice: Hi Alice"""
        content, error = fetch_diagram('mermaid', source)

        assert error is None, f"Unexpected error: {error}"
        assert content, "Content should not be empty"

    def test_fetch_invalid_diagram_type(self):
        """Test that invalid diagram type returns error."""
        source = "digraph G { Hello -> World }"
        content, error = fetch_diagram('invalid_type_xyz', source)

        assert error is not None, "Should return an error for invalid diagram type"
        assert content == b''

    def test_fetch_invalid_syntax(self):
        """Test that invalid diagram syntax returns error."""
        source = "this is not valid graphviz syntax {"
        content, error = fetch_diagram('graphviz', source)

        # Kroki may return 400 or 500 for invalid syntax
        assert error is not None, "Should return an error for invalid syntax"


class TestZeroWidthCharacterInjection:
    """Test zero-width character injection and API behavior."""

    def test_inject_single_char(self):
        """Test injecting a single zero-width character."""
        source = "digraph G { Hello -> World }"
        modified, ok = inject_zero_width_chars(source, "Hello", '\u2060', 1)

        assert ok, "Injection should succeed"
        assert modified == "digraph G { Hello\u2060 -> World }"
        assert len(modified) == len(source) + 1

    def test_inject_multiple_chars(self):
        """Test injecting multiple zero-width characters."""
        source = "digraph G { Hello -> World }"
        modified, ok = inject_zero_width_chars(source, "Hello", '\u2060', 5)

        assert ok, "Injection should succeed"
        assert modified == "digraph G { Hello\u2060\u2060\u2060\u2060\u2060 -> World }"
        assert len(modified) == len(source) + 5

    def test_inject_only_first_occurrence(self):
        """Test that only first occurrence is modified."""
        source = "digraph G { Hello -> Hello }"
        modified, ok = inject_zero_width_chars(source, "Hello", '\u2060', 1)

        assert ok, "Injection should succeed"
        # Count occurrences of zero-width char
        count = modified.count('\u2060')
        assert count == 1, "Should only inject after first occurrence"

    def test_inject_element_not_found(self):
        """Test that injection fails when element is not in source."""
        source = "digraph G { Hello -> World }"
        modified, ok = inject_zero_width_chars(source, "NotFound", '\u2060', 1)

        assert not ok, "Injection should fail when element not found"
        assert modified == source, "Source should be unchanged"

    def test_fetch_with_zero_width_char(self):
        """Test that Kroki accepts diagrams with zero-width characters."""
        source = "digraph G { Hello -> World }"
        modified, ok = inject_zero_width_chars(source, "Hello", '\u2060', 1)
        assert ok, "Injection should succeed"

        content, error = fetch_diagram('graphviz', modified)

        assert error is None, f"Kroki should accept zero-width chars: {error}"
        assert content, "Content should not be empty"
        assert b'<svg' in content.lower() or b'<?xml' in content

    def test_fetch_with_many_zero_width_chars(self):
        """Test that Kroki accepts diagrams with many zero-width characters."""
        source = "digraph G { Hello -> World }"
        modified, ok = inject_zero_width_chars(source, "Hello", '\u2060', 100)
        assert ok, "Injection should succeed"

        content, error = fetch_diagram('graphviz', modified)

        assert error is None, f"Kroki should accept many zero-width chars: {error}"
        assert content, "Content should not be empty"

    @pytest.mark.parametrize("char,name", [
        ('\u2060', 'Word Joiner'),
        ('\u2061', 'Function Application'),
        ('\u2064', 'Invisible Plus'),
    ])
    def test_fetch_with_different_zero_width_chars(self, char, name):
        """Test that Kroki accepts different zero-width characters."""
        source = "digraph G { Hello -> World }"
        modified, ok = inject_zero_width_chars(source, "Hello", char, 10)
        assert ok, "Injection should succeed"

        content, error = fetch_diagram('graphviz', modified)

        assert error is None, f"Kroki should accept {name}: {error}"
        assert content, f"Content should not be empty for {name}"


class TestUrlConstruction:
    """Test that URLs are constructed correctly."""

    def test_url_format(self):
        """Test the URL format matches Kroki API spec."""
        source = "digraph G { A -> B }"
        encoded = encode_kroki(source)

        # Construct URL as the fetch_diagram function does
        url = f"{KROKI_URL}/graphviz/svg/{encoded}"

        # Verify URL is valid by making request
        response = requests.get(url, timeout=30)
        assert response.status_code == 200, f"URL should be valid: {url}"

    def test_url_with_special_chars_in_source(self):
        """Test URL encoding handles special characters."""
        source = 'digraph G { "Node A" -> "Node B" }'
        encoded = encode_kroki(source)
        url = f"{KROKI_URL}/graphviz/svg/{encoded}"

        response = requests.get(url, timeout=30)
        assert response.status_code == 200


class TestDiagramTypes:
    """Test that various diagram types work correctly."""

    @pytest.mark.parametrize("diagram_type,source", [
        ('graphviz', 'digraph G { A -> B }'),
        ('plantuml', '@startuml\nA -> B\n@enduml'),
        ('mermaid', 'graph TD\n    A --> B'),
        ('blockdiag', 'blockdiag { A -> B }'),
        ('seqdiag', 'seqdiag { A -> B }'),
        ('actdiag', 'actdiag { A -> B }'),
        ('nwdiag', 'nwdiag { network { A; B; } }'),
        ('ditaa', '+---+\n| A |\n+---+'),
        ('nomnoml', '[A] -> [B]'),
        ('erd', '[Entity]\n*id'),
        ('d2', 'A -> B'),
        ('svgbob', '+--+\n|A |\n+--+'),
        ('pikchr', 'box "A"'),
    ])
    def test_diagram_type_works(self, diagram_type, source):
        """Test that each diagram type returns valid content."""
        content, error = fetch_diagram(diagram_type, source)

        assert error is None, f"{diagram_type} failed: {error}"
        assert content, f"{diagram_type} returned empty content"
        assert len(content) > 0, f"{diagram_type} returned zero-length content"


if __name__ == '__main__':
    pytest.main([__file__, '-v'])
