/*********************************************************************
* Copyright (c) 2008 The University of York.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.picto.transformers.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Transforms traced text segments (marked with zero-width characters) into
 * span/tspan elements with trace-tag attributes for JavaScript detection.
 *
 * This transformer parses text containing [tag]text[tag] markers and wraps
 * each traced segment in a span (HTML) or tspan (SVG) element.
 *
 * For SVG tspan elements, this transformer also handles cross-element traces
 * where the start and end tags are in different sibling tspan elements.
 */
public class TracedTextWrapperTransformer extends AbstractHtmlElementTransformer {

	private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
	private static final String CROSS_ELEMENT_PROCESSED = "data-cross-element-processed";

	private static final Set<String> SKIP_ELEMENTS = new HashSet<>(Arrays.asList(
		"script", "style", "title", "meta", "link", "noscript",
		"iframe", "object", "embed", "applet"
	));

	protected String zwcChars;

	public TracedTextWrapperTransformer(String zeroWidthChars) {
		this.zwcChars = zeroWidthChars;
	}

	@Override
	public String getXPath() {
		return "//*[text() and not(*)]";
	}

	@Override
	public void transform(Element element) {
		String tagName = element.getTagName().toLowerCase();
		if (SKIP_ELEMENTS.contains(tagName)) {
			return;
		}

		if (hasSkippedAncestor(element)) {
			return;
		}

		// Check for cross-element traces in SVG tspan siblings
		if (isTspanElement(tagName)) {
			Element parent = (Element) element.getParentNode();
			if (parent != null && isTextElement(parent) && !isAlreadyProcessedForCrossElement(parent)) {
				processCrossElementTraces(parent);
			}
		}

		// Skip if this element was already processed as part of a cross-element trace
		if (element.hasAttribute("trace-tag")) {
			return;
		}

		String text = getDirectTextContent(element);
		if (text == null || !containsZwc(text)) {
			return;
		}

		List<TracedSegment> segments = parseTracedSegments(text);
		if (segments.isEmpty() || !hasTracedSegments(segments)) {
			return;
		}

		boolean isSvg = isSvgContext(element);

		rebuildElementContent(element, segments, isSvg);
	}

	private boolean isTspanElement(String tagName) {
		return "tspan".equals(tagName) || tagName.endsWith(":tspan");
	}

	private boolean isTextElement(Element element) {
		String tagName = element.getTagName().toLowerCase();
		return "text".equals(tagName) || tagName.endsWith(":text");
	}

	private boolean isAlreadyProcessedForCrossElement(Element parent) {
		return parent.hasAttribute(CROSS_ELEMENT_PROCESSED);
	}

	/**
	 * Process cross-element traces for all tspan children of a text element.
	 */
	private void processCrossElementTraces(Element textParent) {
		textParent.setAttribute(CROSS_ELEMENT_PROCESSED, "true");

		SiblingTextContext context = new SiblingTextContext(textParent);
		if (context.getElementCount() == 0) {
			return;
		}

		String concatenatedText = context.getConcatenatedText();
		if (!containsZwc(concatenatedText)) {
			return;
		}

		List<CrossElementTrace> crossTraces = parseCrossElementTraces(context);

		for (CrossElementTrace trace : crossTraces) {
			if (trace.isCrossElement()) {
				applyCrossElementTrace(trace, context);
			}
		}
	}

	/**
	 * Parse the concatenated text from siblings to find traces that span multiple elements.
	 */
	private List<CrossElementTrace> parseCrossElementTraces(SiblingTextContext context) {
		List<CrossElementTrace> traces = new ArrayList<>();
		String text = context.getConcatenatedText();

		Integer openTagId = null;
		int openTagEndPos = -1; // Position after the opening tag
		int i = 0;

		while (i < text.length()) {
			if (isZwc(text.charAt(i))) {
				int seqStart = i;
				int seqElementIndex = context.getElementIndexAt(seqStart);
				// Consume ZWC chars but stop at element boundaries so that
				// adjacent tags from different sibling elements are not merged
				while (i < text.length() && isZwc(text.charAt(i))
						&& context.getElementIndexAt(i) == seqElementIndex) {
					i++;
				}
				String zwcSeq = text.substring(seqStart, i);
				int traceId = decodeZwcSequence(zwcSeq);

				if (openTagId == null) {
					openTagId = traceId;
					openTagEndPos = i; // Content starts after the tag
				} else if (traceId == openTagId) {
					// Found matching close tag
					int startElementIndex = context.getElementIndexAt(openTagEndPos);
					int startCharOffset = context.getLocalOffsetAt(openTagEndPos);
					int endElementIndex = context.getElementIndexAt(seqStart);
					int endCharOffset = context.getLocalOffsetAt(seqStart);

					traces.add(new CrossElementTrace(
						traceId,
						startElementIndex, startCharOffset,
						endElementIndex, endCharOffset
					));

					openTagId = null;
					openTagEndPos = -1;
				}
				// If different tag while one is open, ignore (treat as content)
			} else {
				i++;
			}
		}

		return traces;
	}

	/**
	 * Apply a cross-element trace by marking all participating elements.
	 */
	private void applyCrossElementTrace(CrossElementTrace trace, SiblingTextContext context) {
		String groupId = trace.getTraceId() + "-" + UUID.randomUUID().toString().substring(0, 8);
		List<Element> elements = context.getTextElements();

		for (int i = trace.getStartElementIndex(); i <= trace.getEndElementIndex(); i++) {
			Element element = elements.get(i);

			// Determine position within the trace
			String position;
			if (i == trace.getStartElementIndex()) {
				position = "start";
			} else if (i == trace.getEndElementIndex()) {
				position = "end";
			} else {
				position = "middle";
			}

			// Set trace attributes
			element.setAttribute("trace-tag", String.valueOf(trace.getTraceId()));
			element.setAttribute("trace-tag-group", groupId);
			element.setAttribute("trace-position", position);

			// Strip ZWC characters from the element's text content
			stripZwcFromElement(element, trace, i, context);
		}
	}

	/**
	 * Strip ZWC characters from an element that is part of a cross-element trace.
	 */
	private void stripZwcFromElement(Element element, CrossElementTrace trace,
			int elementIndex, SiblingTextContext context) {
		String text = getDirectTextContent(element);
		if (text == null) return;

		StringBuilder newText = new StringBuilder();
		int i = 0;

		while (i < text.length()) {
			if (isZwc(text.charAt(i))) {
				// Skip ZWC sequences
				while (i < text.length() && isZwc(text.charAt(i))) {
					i++;
				}
			} else {
				newText.append(text.charAt(i));
				i++;
			}
		}

		// Replace text content
		setDirectTextContent(element, newText.toString());
	}

	private void setDirectTextContent(Element element, String newText) {
		// Remove existing text nodes
		List<Node> textNodesToRemove = new ArrayList<>();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.TEXT_NODE) {
				textNodesToRemove.add(children.item(i));
			}
		}
		for (Node textNode : textNodesToRemove) {
			element.removeChild(textNode);
		}

		// Add new text node
		if (!newText.isEmpty()) {
			Node firstChild = element.getFirstChild();
			Node newTextNode = element.getOwnerDocument().createTextNode(newText);
			if (firstChild != null) {
				element.insertBefore(newTextNode, firstChild);
			} else {
				element.appendChild(newTextNode);
			}
		}
	}

	private boolean hasSkippedAncestor(Element element) {
		Node parent = element.getParentNode();
		while (parent != null && parent instanceof Element) {
			String name = ((Element) parent).getTagName().toLowerCase();
			if (SKIP_ELEMENTS.contains(name)) {
				return true;
			}
			parent = parent.getParentNode();
		}
		return false;
	}

	private String getDirectTextContent(Element element) {
		StringBuilder sb = new StringBuilder();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE) {
				sb.append(child.getTextContent());
			}
		}
		return sb.length() > 0 ? sb.toString() : null;
	}

	private boolean containsZwc(String text) {
		for (int i = 0; i < zwcChars.length(); i++) {
			if (text.indexOf(zwcChars.charAt(i)) >= 0) {
				return true;
			}
		}
		return false;
	}

	private boolean isZwc(char c) {
		return zwcChars.indexOf(c) >= 0;
	}

	private int zwcToDigit(char c) {
		return zwcChars.indexOf(c);
	}

	private int decodeZwcSequence(String seq) {
		int id = 0;
		for (int i = 0; i < seq.length(); i++) {
			int digit = zwcToDigit(seq.charAt(i));
			if (digit < 0) return -1;
			id = id * zwcChars.length() + digit;
		}
		return id;
	}

	private boolean hasTracedSegments(List<TracedSegment> segments) {
		return segments.stream().anyMatch(TracedSegment::isTraced);
	}

	private boolean isSvgContext(Element element) {
		Node current = element;
		while (current != null) {
			if (current instanceof Element) {
				String name = ((Element) current).getTagName().toLowerCase();
				if ("svg".equals(name)) return true;
				String ns = ((Element) current).getNamespaceURI();
				if (SVG_NAMESPACE.equals(ns)) return true;
			}
			current = current.getParentNode();
		}
		return false;
	}

	/**
	 * Parse text into traced and non-traced segments.
	 * Handles: [tag]text[tag] format where tags are base-5 encoded ZWC sequences.
	 */
	private List<TracedSegment> parseTracedSegments(String text) {
		List<TracedSegment> segments = new ArrayList<>();
		StringBuilder currentText = new StringBuilder();
		Integer currentTraceId = null;
		int i = 0;

		while (i < text.length()) {
			if (isZwc(text.charAt(i))) {
				int seqStart = i;
				while (i < text.length() && isZwc(text.charAt(i))) {
					i++;
				}
				String zwcSeq = text.substring(seqStart, i);
				int traceId = decodeZwcSequence(zwcSeq);

				if (currentTraceId == null) {
					if (currentText.length() > 0) {
						segments.add(new TracedSegment(currentText.toString(), null));
						currentText = new StringBuilder();
					}
					currentTraceId = traceId;
				} else if (traceId == currentTraceId) {
					segments.add(new TracedSegment(currentText.toString(), currentTraceId));
					currentText = new StringBuilder();
					currentTraceId = null;
				} else {
					currentText.append(zwcSeq);
				}
			} else {
				currentText.append(text.charAt(i));
				i++;
			}
		}

		if (currentText.length() > 0) {
			segments.add(new TracedSegment(currentText.toString(), null));
		}

		return segments;
	}

	/**
	 * Rebuild element content, wrapping traced segments in span/tspan.
	 */
	private void rebuildElementContent(Element element, List<TracedSegment> segments, boolean isSvg) {
		Document doc = element.getOwnerDocument();

		List<Node> textNodesToRemove = new ArrayList<>();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.TEXT_NODE) {
				textNodesToRemove.add(children.item(i));
			}
		}
		for (Node textNode : textNodesToRemove) {
			element.removeChild(textNode);
		}

		Node firstChild = element.getFirstChild();

		for (TracedSegment segment : segments) {
			Node newNode;
			if (segment.isTraced()) {
				Element wrapper;
				if (isSvg) {
					wrapper = doc.createElementNS(SVG_NAMESPACE, "tspan");
				} else {
					wrapper = doc.createElement("span");
				}
				wrapper.setAttribute("trace-tag", String.valueOf(segment.getTraceId()));
				wrapper.setTextContent(segment.getText());
				newNode = wrapper;
			} else {
				newNode = doc.createTextNode(segment.getText());
			}

			if (firstChild != null) {
				element.insertBefore(newNode, firstChild);
			} else {
				element.appendChild(newNode);
			}
		}
	}
}
