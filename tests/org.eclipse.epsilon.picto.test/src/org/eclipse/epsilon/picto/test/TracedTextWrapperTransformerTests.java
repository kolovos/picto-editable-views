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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.eclipse.epsilon.picto.transformers.elements.TracedSegment;
import org.eclipse.epsilon.picto.transformers.elements.TracedTextWrapperTransformer;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Tests for TracedTextWrapperTransformer and TracedSegment.
 */
public class TracedTextWrapperTransformerTests {

	private static final String ZWC = "\u2060";

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
		// Input: <div>[tag1]Hello[tag1]</div> where tag1 = one ZWC
		String html = "<html><body><div>" + ZWC + "Hello" + ZWC + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC);

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
		// tag1 = one ZWC, tag2 = two ZWCs
		String tag1 = ZWC;
		String tag2 = ZWC + ZWC;
		String html = "<html><body><div>" + tag1 + "Author" + tag1 + ", Title: " + tag2 + "Book" + tag2 + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC);

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
		String html = "<html><body><div>Prefix: " + ZWC + "Value" + ZWC + " suffix</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC);

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
	public void testEmptyTracedValue() throws Exception {
		// Input: <div>[tag1][tag1]</div> (empty traced text)
		String html = "<html><body><div>" + ZWC + "" + ZWC + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC);

		NodeList divs = getElements(doc, "//div");
		transformer.transform((Element) divs.item(0));

		// Verify one span with empty content
		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(1, spans.getLength());

		Element span = (Element) spans.item(0);
		assertEquals("1", span.getAttribute("trace-tag"));
		assertEquals("", span.getTextContent());
	}

	@Test
	public void testNoTransformationWhenNoZwc() throws Exception {
		// Input without any ZWC characters should not be transformed
		String html = "<html><body><div>Plain text</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC);

		NodeList divs = getElements(doc, "//div");
		transformer.transform((Element) divs.item(0));

		// Should have no spans
		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(0, spans.getLength());
	}

	@Test
	public void testSkipsScriptElements() throws Exception {
		// Script elements should never be transformed
		String html = "<html><body><script>" + ZWC + "code" + ZWC + "</script></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC);

		NodeList scripts = getElements(doc, "//script");
		transformer.transform((Element) scripts.item(0));

		// Should have no spans inside script
		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(0, spans.getLength());
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
}
