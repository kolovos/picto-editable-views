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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.eclipse.epsilon.picto.trace.TraceManager;
import org.eclipse.epsilon.picto.transformers.elements.TracedSegment;
import org.eclipse.epsilon.picto.transformers.elements.TracedTextWrapperTransformer;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Tests for TracedTextWrapperTransformer and TracedSegment.
 */
public class TracedTextWrapperTransformerTests {

	// All 5 ZWC characters used for base-5 encoding
	private static final String ZWC_CHARS = "\u2060\u2061\u2062\u2063\u2064";

	@Test
	public void testTracedSegmentBasic() {
		TracedSegment traced = new TracedSegment("Hello", 1);
		assertTrue(traced.isTraced());
		assertEquals("Hello", traced.getText());
		assertEquals(Integer.valueOf(1), traced.getTraceId());
	}

	@Test
	public void testTracedSegmentNonTraced() {
		TracedSegment nonTraced = new TracedSegment("World", null);
		assertFalse(nonTraced.isTraced());
		assertEquals("World", nonTraced.getText());
		assertNull(nonTraced.getTraceId());
	}

	@Test
	public void testSingleTracedText() throws Exception {
		// Input: <div>[tag1]Hello[tag1]</div> where tag1 = base-5 encoding of ID 1
		String tag1 = new TraceManager().idToTag(1);
		String html = "<html><body><div>" + tag1 + "Hello" + tag1 + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		// Find and transform the div
		NodeList divs = getElements(doc, "//div");
		assertEquals(1, divs.getLength());
		transformer.transform((Element) divs.item(0));

		// Verify the result has a span with trace-tag attribute
		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(1, spans.getLength());

		Element span = (Element) spans.item(0);
		assertEquals("1", span.getAttribute("trace-tag"));
		assertEquals("Hello", span.getTextContent());
	}

	@Test
	public void testMultipleTracedTexts() throws Exception {
		// Input: <div>[tag1]Author[tag1], Title: [tag2]Book[tag2]</div>
		// Using base-5 encoded tags
		String tag1 = new TraceManager().idToTag(1);
		String tag2 = new TraceManager().idToTag(2);
		String html = "<html><body><div>" + tag1 + "Author" + tag1 + ", Title: " + tag2 + "Book" + tag2 + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		// Find and transform the div
		NodeList divs = getElements(doc, "//div");
		transformer.transform((Element) divs.item(0));

		// Verify two spans with different trace-tag values
		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(2, spans.getLength());

		Element span1 = (Element) spans.item(0);
		Element span2 = (Element) spans.item(1);

		assertEquals("1", span1.getAttribute("trace-tag"));
		assertEquals("Author", span1.getTextContent());

		assertEquals("2", span2.getAttribute("trace-tag"));
		assertEquals("Book", span2.getTextContent());
	}

	@Test
	public void testMixedTracedAndNonTracedText() throws Exception {
		// Input: <div>Prefix: [tag1]Value[tag1] suffix</div>
		String tag1 = new TraceManager().idToTag(1);
		String html = "<html><body><div>Prefix: " + tag1 + "Value" + tag1 + " suffix</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList divs = getElements(doc, "//div");
		transformer.transform((Element) divs.item(0));

		// Verify one span
		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(1, spans.getLength());

		Element span = (Element) spans.item(0);
		assertEquals("1", span.getAttribute("trace-tag"));
		assertEquals("Value", span.getTextContent());

		// The div should contain: text node "Prefix: ", span, text node " suffix"
		Element div = (Element) divs.item(0);
		assertTrue(div.getTextContent().contains("Prefix:"));
		assertTrue(div.getTextContent().contains("Value"));
		assertTrue(div.getTextContent().contains("suffix"));
	}

	@Test
	public void testEmptyTracedValueNotSupported() throws Exception {
		// With base-5 encoding, adjacent identical tags merge into a single sequence.
		// So [tag1][tag1] (empty traced text) becomes a single longer ZWC sequence
		// that doesn't match any opening tag, resulting in no traced segments.
		String tag1 = new TraceManager().idToTag(1);
		String html = "<html><body><div>" + tag1 + "" + tag1 + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList divs = getElements(doc, "//div");
		transformer.transform((Element) divs.item(0));

		// Adjacent identical tags merge, so no valid traced segments are detected
		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(0, spans.getLength());
	}

	@Test
	public void testNonEmptyTracedValue() throws Exception {
		// A traced value with at least one character works correctly
		String tag1 = new TraceManager().idToTag(1);
		String html = "<html><body><div>" + tag1 + "X" + tag1 + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList divs = getElements(doc, "//div");
		transformer.transform((Element) divs.item(0));

		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(1, spans.getLength());

		Element span = (Element) spans.item(0);
		assertEquals("1", span.getAttribute("trace-tag"));
		assertEquals("X", span.getTextContent());
	}

	@Test
	public void testNoTransformationWhenNoZwc() throws Exception {
		// Input without any ZWC characters should not be transformed
		String html = "<html><body><div>Plain text</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList divs = getElements(doc, "//div");
		transformer.transform((Element) divs.item(0));

		// Should have no spans
		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(0, spans.getLength());
	}

	@Test
	public void testSkipsScriptElements() throws Exception {
		// Script elements should never be transformed
		String tag1 = new TraceManager().idToTag(1);
		String html = "<html><body><script>" + tag1 + "code" + tag1 + "</script></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList scripts = getElements(doc, "//script");
		transformer.transform((Element) scripts.item(0));

		// Should have no spans inside script
		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(0, spans.getLength());
	}

	@Test
	public void testLargeTraceId() throws Exception {
		// Test with a larger trace ID (100) to verify base-5 encoding works
		String tag100 = new TraceManager().idToTag(100);
		String html = "<html><body><div>" + tag100 + "Large ID" + tag100 + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList divs = getElements(doc, "//div");
		transformer.transform((Element) divs.item(0));

		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(1, spans.getLength());

		Element span = (Element) spans.item(0);
		assertEquals("100", span.getAttribute("trace-tag"));
		assertEquals("Large ID", span.getTextContent());
	}

	@Test
	public void testMultipleLargeTraceIds() throws Exception {
		// Test with multiple larger trace IDs
		String tag25 = new TraceManager().idToTag(25);
		String tag100 = new TraceManager().idToTag(100);
		String html = "<html><body><div>" + tag25 + "First" + tag25 + " and " + tag100 + "Second" + tag100 + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList divs = getElements(doc, "//div");
		transformer.transform((Element) divs.item(0));

		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(2, spans.getLength());

		Element span1 = (Element) spans.item(0);
		Element span2 = (Element) spans.item(1);

		assertEquals("25", span1.getAttribute("trace-tag"));
		assertEquals("First", span1.getTextContent());

		assertEquals("100", span2.getAttribute("trace-tag"));
		assertEquals("Second", span2.getTextContent());
	}

	// ======== Cross-Element Trace Tests ========

	@Test
	public void testCrossElementTraceInSvgTwoTspans() throws Exception {
		// Trace spans two tspan elements: [tag]Line1 | Line2[tag]
		String tag1 = new TraceManager().idToTag(1);
		String svg = "<html><body><svg><text>" +
			"<tspan>" + tag1 + "Line 1</tspan>" +
			"<tspan>Line 2" + tag1 + "</tspan>" +
			"</text></svg></body></html>";
		Document doc = parseHtml(svg);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		// Transform first tspan (triggers cross-element processing)
		NodeList tspans = getElements(doc, "//tspan");
		transformer.transform((Element) tspans.item(0));

		// Both tspans should have trace-tag and trace-tag-group attributes
		NodeList tracedTspans = getElements(doc, "//tspan[@trace-tag]");
		assertEquals(2, tracedTspans.getLength());

		Element tspan1 = (Element) tracedTspans.item(0);
		Element tspan2 = (Element) tracedTspans.item(1);

		// All should have the same trace-tag value
		assertEquals("1", tspan1.getAttribute("trace-tag"));
		assertEquals("1", tspan2.getAttribute("trace-tag"));

		// All should have the same trace-tag-group value
		String groupId = tspan1.getAttribute("trace-tag-group");
		assertNotNull(groupId);
		assertFalse(groupId.isEmpty());
		assertEquals(groupId, tspan2.getAttribute("trace-tag-group"));

		// Check positions
		assertEquals("start", tspan1.getAttribute("trace-position"));
		assertEquals("end", tspan2.getAttribute("trace-position"));

		// ZWC characters should be stripped from text content
		assertEquals("Line 1", tspan1.getTextContent());
		assertEquals("Line 2", tspan2.getTextContent());
	}

	@Test
	public void testCrossElementTraceInSvgThreeTspans() throws Exception {
		// Trace spans three tspan elements
		String tag1 = new TraceManager().idToTag(1);
		String svg = "<html><body><svg><text>" +
			"<tspan>" + tag1 + "Line 1</tspan>" +
			"<tspan>Line 2</tspan>" +
			"<tspan>Line 3" + tag1 + "</tspan>" +
			"</text></svg></body></html>";
		Document doc = parseHtml(svg);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList tspans = getElements(doc, "//tspan");
		transformer.transform((Element) tspans.item(0));

		// All three tspans should be marked
		NodeList tracedTspans = getElements(doc, "//tspan[@trace-tag]");
		assertEquals(3, tracedTspans.getLength());

		Element tspan1 = (Element) tracedTspans.item(0);
		Element tspan2 = (Element) tracedTspans.item(1);
		Element tspan3 = (Element) tracedTspans.item(2);

		// Same group ID for all
		String groupId = tspan1.getAttribute("trace-tag-group");
		assertEquals(groupId, tspan2.getAttribute("trace-tag-group"));
		assertEquals(groupId, tspan3.getAttribute("trace-tag-group"));

		// Check positions
		assertEquals("start", tspan1.getAttribute("trace-position"));
		assertEquals("middle", tspan2.getAttribute("trace-position"));
		assertEquals("end", tspan3.getAttribute("trace-position"));
	}

	@Test
	public void testMixedSingleAndCrossElementTraces() throws Exception {
		// First trace spans two elements, second trace is in a single element
		String tag1 = new TraceManager().idToTag(1);
		String tag2 = new TraceManager().idToTag(2);
		String svg = "<html><body><svg><text>" +
			"<tspan>" + tag1 + "Start</tspan>" +
			"<tspan>End" + tag1 + "</tspan>" +
			"<tspan>" + tag2 + "Single" + tag2 + "</tspan>" +
			"</text></svg></body></html>";
		Document doc = parseHtml(svg);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		// Transform all tspans
		NodeList tspans = getElements(doc, "//tspan");
		for (int i = 0; i < tspans.getLength(); i++) {
			transformer.transform((Element) tspans.item(i));
		}

		// First two tspans should be a cross-element group
		NodeList groupedElements = getElementsWithAttribute(doc, "trace-tag-group");
		assertEquals(2, groupedElements.getLength());

		// Third tspan should contain a nested element with trace-tag (from single element trace)
		Element tspan3 = (Element) tspans.item(2);
		// The trace is inside tspan3, so check for child element with trace-tag
		NodeList tspan3Children = tspan3.getChildNodes();
		Element tracedChild = null;
		for (int i = 0; i < tspan3Children.getLength(); i++) {
			if (tspan3Children.item(i) instanceof Element) {
				Element child = (Element) tspan3Children.item(i);
				if (child.hasAttribute("trace-tag")) {
					tracedChild = child;
					break;
				}
			}
		}
		assertNotNull("Third tspan should contain a traced child element", tracedChild);
		assertEquals("2", tracedChild.getAttribute("trace-tag"));
		assertFalse(tracedChild.hasAttribute("trace-tag-group"));

		// Verify trace IDs for grouped elements
		assertEquals("1", ((Element) groupedElements.item(0)).getAttribute("trace-tag"));
		assertEquals("1", ((Element) groupedElements.item(1)).getAttribute("trace-tag"));
	}

	@Test
	public void testSingleElementTraceInTspan() throws Exception {
		// Trace is fully contained in a single tspan - should not create a group
		String tag1 = new TraceManager().idToTag(1);
		String svg = "<html><body><svg><text>" +
			"<tspan>" + tag1 + "Complete" + tag1 + "</tspan>" +
			"</text></svg></body></html>";
		Document doc = parseHtml(svg);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList tspans = getElements(doc, "//tspan");
		Element originalTspan = (Element) tspans.item(0);
		transformer.transform(originalTspan);

		// After transformation, the original tspan should contain a nested tspan with trace-tag
		// Find elements with trace-tag attribute by iterating through descendants
		NodeList tracedElements = getElementsWithAttribute(doc, "trace-tag");
		assertEquals(1, tracedElements.getLength());

		Element traced = (Element) tracedElements.item(0);
		assertEquals("1", traced.getAttribute("trace-tag"));
		assertFalse(traced.hasAttribute("trace-tag-group"));
		assertEquals("Complete", traced.getTextContent());
	}

	@Test
	public void testUnmatchedTagsIgnored() throws Exception {
		// Opening tag with no matching close tag should be ignored
		String tag1 = new TraceManager().idToTag(1);
		String svg = "<html><body><svg><text>" +
			"<tspan>" + tag1 + "Line 1</tspan>" +
			"<tspan>Line 2</tspan>" +
			"</text></svg></body></html>";
		Document doc = parseHtml(svg);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList tspans = getElements(doc, "//tspan");
		transformer.transform((Element) tspans.item(0));

		// No traces should be created (unmatched tags)
		NodeList tracedTspans = getElements(doc, "//tspan[@trace-tag]");
		assertEquals(0, tracedTspans.getLength());
	}

	@Test
	public void testCrossElementTraceWithLargeId() throws Exception {
		// Cross-element trace with a larger ID (100)
		String tag100 = new TraceManager().idToTag(100);
		String svg = "<html><body><svg><text>" +
			"<tspan>" + tag100 + "Start</tspan>" +
			"<tspan>End" + tag100 + "</tspan>" +
			"</text></svg></body></html>";
		Document doc = parseHtml(svg);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList tspans = getElements(doc, "//tspan");
		transformer.transform((Element) tspans.item(0));

		NodeList tracedTspans = getElements(doc, "//tspan[@trace-tag]");
		assertEquals(2, tracedTspans.getLength());

		assertEquals("100", ((Element) tracedTspans.item(0)).getAttribute("trace-tag"));
		assertEquals("100", ((Element) tracedTspans.item(1)).getAttribute("trace-tag"));
	}

	private Document parseHtml(String html) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(html)));
	}

	private NodeList getElements(Document doc, String xpath) throws Exception {
		XPath xPath = XPathFactory.newInstance().newXPath();
		return (NodeList) xPath.compile(xpath).evaluate(doc, XPathConstants.NODESET);
	}

	/**
	 * Find all elements with a given attribute, regardless of namespace.
	 * This is useful for SVG elements which may be in a namespace that XPath doesn't handle well.
	 */
	private NodeList getElementsWithAttribute(Document doc, String attributeName) {
		List<Element> result = new ArrayList<>();
		collectElementsWithAttribute(doc.getDocumentElement(), attributeName, result);
		return new SimpleNodeList(result);
	}

	private void collectElementsWithAttribute(Element element, String attributeName, List<Element> result) {
		if (element.hasAttribute(attributeName)) {
			result.add(element);
		}
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Element) {
				collectElementsWithAttribute((Element) children.item(i), attributeName, result);
			}
		}
	}

	private static class SimpleNodeList implements NodeList {
		private final List<Element> elements;

		SimpleNodeList(List<Element> elements) {
			this.elements = elements;
		}

		@Override
		public Node item(int index) {
			return elements.get(index);
		}

		@Override
		public int getLength() {
			return elements.size();
		}
	}
}
