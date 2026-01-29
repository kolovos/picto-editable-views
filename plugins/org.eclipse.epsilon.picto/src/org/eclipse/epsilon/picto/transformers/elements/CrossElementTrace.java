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

/**
 * Represents a trace that may span multiple sibling elements.
 * Used when a traced string spans multiple tspan elements in SVG.
 */
public class CrossElementTrace {

	private final int traceId;
	private final int startElementIndex;
	private final int startCharOffset;
	private final int endElementIndex;
	private final int endCharOffset;

	/**
	 * Creates a new cross-element trace.
	 *
	 * @param traceId           The numeric trace ID
	 * @param startElementIndex Index of the element containing the start tag (0-based)
	 * @param startCharOffset   Character offset within the start element where traced content begins
	 * @param endElementIndex   Index of the element containing the end tag (0-based)
	 * @param endCharOffset     Character offset within the end element where traced content ends
	 */
	public CrossElementTrace(int traceId, int startElementIndex, int startCharOffset,
			int endElementIndex, int endCharOffset) {
		this.traceId = traceId;
		this.startElementIndex = startElementIndex;
		this.startCharOffset = startCharOffset;
		this.endElementIndex = endElementIndex;
		this.endCharOffset = endCharOffset;
	}

	public int getTraceId() {
		return traceId;
	}

	public int getStartElementIndex() {
		return startElementIndex;
	}

	public int getStartCharOffset() {
		return startCharOffset;
	}

	public int getEndElementIndex() {
		return endElementIndex;
	}

	public int getEndCharOffset() {
		return endCharOffset;
	}

	/**
	 * Returns true if this trace spans multiple elements.
	 */
	public boolean isCrossElement() {
		return startElementIndex != endElementIndex;
	}

	@Override
	public String toString() {
		return "CrossElementTrace[id=" + traceId + ", start=(" + startElementIndex + "," + startCharOffset
				+ "), end=(" + endElementIndex + "," + endCharOffset + ")]";
	}
}
