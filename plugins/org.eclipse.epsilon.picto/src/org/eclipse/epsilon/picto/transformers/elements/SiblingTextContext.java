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
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Context for processing sibling text elements together.
 * Holds the parent element, child text elements, and provides
 * character position mapping between concatenated text and individual elements.
 */
public class SiblingTextContext {

	private final Element parent;
	private final List<Element> textElements;
	private final String concatenatedText;
	private final int[] elementStartPositions;

	/**
	 * Creates a context for the given parent element by collecting
	 * all child elements that contain text (e.g., tspan elements).
	 */
	public SiblingTextContext(Element parent) {
		this.parent = parent;
		this.textElements = collectTextElements(parent);

		StringBuilder sb = new StringBuilder();
		this.elementStartPositions = new int[textElements.size()];

		for (int i = 0; i < textElements.size(); i++) {
			elementStartPositions[i] = sb.length();
			sb.append(getDirectTextContent(textElements.get(i)));
		}

		this.concatenatedText = sb.toString();
	}

	private List<Element> collectTextElements(Element parent) {
		List<Element> elements = new ArrayList<>();
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element elem = (Element) child;
				String tagName = elem.getTagName().toLowerCase();
				// Collect SVG text-bearing siblings for cross-element trace handling.
				if ("tspan".equals(tagName) || tagName.endsWith(":tspan")
						|| "text".equals(tagName) || tagName.endsWith(":text")) {
					elements.add(elem);
				}
			}
		}
		return elements;
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
		return sb.toString();
	}

	public Element getParent() {
		return parent;
	}

	public List<Element> getTextElements() {
		return Collections.unmodifiableList(textElements);
	}

	public String getConcatenatedText() {
		return concatenatedText;
	}

	public int getElementCount() {
		return textElements.size();
	}

	/**
	 * Maps a character position in the concatenated text to an element index.
	 */
	public int getElementIndexAt(int charPosition) {
		for (int i = textElements.size() - 1; i >= 0; i--) {
			if (charPosition >= elementStartPositions[i]) {
				return i;
			}
		}
		return 0;
	}

	/**
	 * Maps a character position in the concatenated text to a local offset
	 * within the element at that position.
	 */
	public int getLocalOffsetAt(int charPosition) {
		int elementIndex = getElementIndexAt(charPosition);
		return charPosition - elementStartPositions[elementIndex];
	}

	/**
	 * Gets the start position of the given element in the concatenated text.
	 */
	public int getElementStartPosition(int elementIndex) {
		return elementStartPositions[elementIndex];
	}

	/**
	 * Gets the text content of a specific element.
	 */
	public String getElementText(int elementIndex) {
		return getDirectTextContent(textElements.get(elementIndex));
	}
}
