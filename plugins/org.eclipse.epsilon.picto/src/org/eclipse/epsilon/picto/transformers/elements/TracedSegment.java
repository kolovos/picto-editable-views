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
 * Represents a segment of text that may be traced.
 * Used by TracedTextWrapperTransformer to parse text containing
 * zero-width character markers.
 */
public class TracedSegment {

	private final String text;
	private final Integer traceId;  // null if not traced

	public TracedSegment(String text, Integer traceId) {
		this.text = text;
		this.traceId = traceId;
	}

	public boolean isTraced() {
		return traceId != null;
	}

	public String getText() {
		return text;
	}

	public Integer getTraceId() {
		return traceId;
	}

	@Override
	public String toString() {
		return isTraced() ? "[" + traceId + "]" + text + "[/" + traceId + "]" : text;
	}
}
