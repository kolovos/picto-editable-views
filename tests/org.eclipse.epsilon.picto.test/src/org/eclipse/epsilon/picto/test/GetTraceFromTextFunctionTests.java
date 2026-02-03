/*********************************************************************
* Copyright (c) 2020 The University of York.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.picto.test;

import static org.junit.Assert.*;

import org.eclipse.epsilon.picto.trace.GetTraceFromTextFunction;
import org.eclipse.epsilon.picto.trace.TraceManager;
import org.junit.Test;

/**
 * Tests for GetTraceFromTextFunction which decodes ZWC-encoded traces from text.
 */
public class GetTraceFromTextFunctionTests {

	// All 5 ZWC characters used for base-5 encoding
	private static final String ZWC_CHARS = "\u2060\u2061\u2062\u2063\u2064";

	@Test
	public void testBracketedFormat() {
		// [tag1]Hello[tag1] format
		String tag1 = TraceManager.idToTag(1);
		String text = tag1 + "Hello" + tag1;

		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("1", result);
	}

	@Test
	public void testBracketedFormatLargeId() {
		// [tag100]Value[tag100] format with larger ID
		String tag100 = TraceManager.idToTag(100);
		String text = tag100 + "Value" + tag100;

		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("100", result);
	}

	@Test
	public void testSuffixFormat() {
		// text[tag1] format (legacy)
		String tag1 = TraceManager.idToTag(1);
		String text = "Hello" + tag1;

		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("1", result);
	}

	@Test
	public void testSuffixFormatLargeId() {
		// text[tag100] format with larger ID
		String tag100 = TraceManager.idToTag(100);
		String text = "Value" + tag100;

		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("100", result);
	}

	@Test
	public void testNoTrace() {
		// Plain text without any ZWC markers
		String result = GetTraceFromTextFunction.getTraceFromText("Plain text", ZWC_CHARS);
		assertEquals("", result);
	}

	@Test
	public void testEmptyText() {
		String result = GetTraceFromTextFunction.getTraceFromText("", ZWC_CHARS);
		assertEquals("", result);
	}

	@Test
	public void testNullText() {
		String result = GetTraceFromTextFunction.getTraceFromText(null, ZWC_CHARS);
		assertEquals("", result);
	}

	@Test
	public void testNullZwcChars() {
		String result = GetTraceFromTextFunction.getTraceFromText("Hello", null);
		assertEquals("", result);
	}

	@Test
	public void testEmptyZwcChars() {
		String result = GetTraceFromTextFunction.getTraceFromText("Hello", "");
		assertEquals("", result);
	}

	@Test
	public void testMixedTracedAndNonTracedText() {
		// Prefix: [tag1]Value[tag1] suffix
		String tag1 = TraceManager.idToTag(1);
		String text = "Prefix: " + tag1 + "Value" + tag1 + " suffix";

		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("1", result);
	}

	@Test
	public void testMultipleTracesReturnsFirst() {
		// [tag1]First[tag1] and [tag2]Second[tag2]
		String tag1 = TraceManager.idToTag(1);
		String tag2 = TraceManager.idToTag(2);
		String text = tag1 + "First" + tag1 + " and " + tag2 + "Second" + tag2;

		// Should return the first trace found
		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("1", result);
	}

	@Test
	public void testUnmatchedOpeningTag() {
		// [tag1]Hello without closing tag - should fall back to suffix detection
		String tag1 = TraceManager.idToTag(1);
		String text = tag1 + "Hello";

		// No valid bracketed trace, no suffix - returns empty
		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("", result);
	}

	@Test
	public void testBracketedPreferredOverSuffix() {
		// [tag1]Value[tag1] text [tag2] - bracketed format should be detected first
		// Note: Adjacent ZWC sequences merge, so we need non-ZWC separator
		String tag1 = TraceManager.idToTag(1);
		String tag2 = TraceManager.idToTag(2);
		String text = tag1 + "Value" + tag1 + " " + tag2;

		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("1", result);
	}

	@Test
	public void testAdjacentTagsMerge() {
		// When [tag1][tag2] are adjacent (no separator), they merge into a single
		// ZWC sequence that doesn't match the opening tag. Falls back to suffix.
		String tag1 = TraceManager.idToTag(1);
		String tag2 = TraceManager.idToTag(2);
		String text = tag1 + "Value" + tag1 + tag2;

		// tag1+tag2 adjacent decodes as: 1*5 + 2 = 7 (in base-5)
		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("7", result);
	}

	@Test
	public void testZeroId() {
		// ID 0 should work (encodes to single ZWC char)
		String tag0 = TraceManager.idToTag(0);
		String text = tag0 + "Zero" + tag0;

		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("0", result);
	}

	// ========== Multi-line trace detection tests ==========
	// These test scenarios where traced strings span multiple lines/elements

	@Test
	public void testMultiLineTraceWithNewlines() {
		// [tag]Line one\nLine two[tag] - text spans multiple lines
		String tag1 = TraceManager.idToTag(1);
		String text = tag1 + "Line one\nLine two" + tag1;

		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("1", result);
	}

	@Test
	public void testMultiLineConcatenatedFromTspans() {
		// Simulates concatenated text from multiple tspan elements:
		// <tspan>[tag]Line one</tspan><tspan>Line two[tag]</tspan>
		String tag1 = TraceManager.idToTag(1);
		String tspan1Text = tag1 + "Line one";
		String tspan2Text = "Line two" + tag1;
		String concatenated = tspan1Text + tspan2Text;

		String result = GetTraceFromTextFunction.getTraceFromText(concatenated, ZWC_CHARS);
		assertEquals("1", result);
	}

	@Test
	public void testMultiLineWithWhitespace() {
		// Text with leading/trailing whitespace from formatting
		String tag1 = TraceManager.idToTag(1);
		String text = "   " + tag1 + "Line one\n    Line two" + tag1 + "   ";

		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("1", result);
	}

	@Test
	public void testPartialTagAtStart() {
		// Only opening tag present (simulates hovering first tspan only)
		String tag1 = TraceManager.idToTag(1);
		String text = tag1 + "Line one";

		// No complete trace - should return empty
		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("", result);
	}

	@Test
	public void testPartialTagAtEnd() {
		// Only closing tag present (simulates hovering last tspan only)
		String tag1 = TraceManager.idToTag(1);
		String text = "Line two" + tag1;

		// Has suffix format - should detect as suffix trace
		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("1", result);
	}

	@Test
	public void testMultiLineWithLargeId() {
		// Multi-line with larger ID (tests base-5 encoding across lines)
		String tag500 = TraceManager.idToTag(500);
		String text = tag500 + "First line\nSecond line\nThird line" + tag500;

		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("500", result);
	}

	@Test
	public void testNestedMultiLineTraces() {
		// Multiple traces in concatenated multi-line text
		String tag1 = TraceManager.idToTag(1);
		String tag2 = TraceManager.idToTag(2);
		String text = tag1 + "Outer start\n" + tag2 + "Inner" + tag2 + "\nOuter end" + tag1;

		// Should return first complete trace (tag1)
		String result = GetTraceFromTextFunction.getTraceFromText(text, ZWC_CHARS);
		assertEquals("1", result);
	}
}
