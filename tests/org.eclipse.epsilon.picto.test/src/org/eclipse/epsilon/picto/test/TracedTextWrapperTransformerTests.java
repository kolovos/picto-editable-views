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

import org.eclipse.epsilon.picto.trace.TraceManager;
import org.eclipse.epsilon.picto.transformers.elements.TracedTextWrapperTransformer;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Tests for TracedTextWrapperTransformer.
 *
 * The transformer is now a no-op - ZWC markers are kept in text and
 * JavaScript calls the getTraceFromText() Java function to decode them.
 */
public class TracedTextWrapperTransformerTests {

	private static final String ZWC_CHARS = "\u2060\u2061\u2062\u2063\u2064";

	@Test
	public void testTransformerIsNoOp() throws Exception {
		// The transformer should not modify the document
		String tag1 = TraceManager.idToTag(1);
		String html = "<html><body><div>" + tag1 + "Hello" + tag1 + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		// XPath matches nothing, so transform is never called on real elements
		assertEquals("/nonexistent", transformer.getXPath());

		// Even if we call transform manually, it should do nothing
		NodeList divs = getElements(doc, "//div");
		Element div = (Element) divs.item(0);
		String originalContent = div.getTextContent();

		transformer.transform(div);

		// Content should be unchanged (ZWC markers preserved)
		assertEquals(originalContent, div.getTextContent());

		// No span elements should have been created
		NodeList spans = getElements(doc, "//span[@trace-tag]");
		assertEquals(0, spans.getLength());
	}

	@Test
	public void testZwcMarkersPreserved() throws Exception {
		// ZWC markers should remain in the text for JS to read via Java callback
		String tag1 = TraceManager.idToTag(1);
		String expectedMarkers = tag1 + "Hello" + tag1;
		String html = "<html><body><div>" + expectedMarkers + "</div></body></html>";
		Document doc = parseHtml(html);

		TracedTextWrapperTransformer transformer = new TracedTextWrapperTransformer(ZWC_CHARS);

		NodeList divs = getElements(doc, "//div");
		Element div = (Element) divs.item(0);

		transformer.transform(div);

		// The text should still contain the ZWC markers
		String textContent = div.getTextContent();
		assertTrue("ZWC markers should be preserved", textContent.contains(tag1));
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
