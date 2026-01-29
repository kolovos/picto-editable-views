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

import org.w3c.dom.Element;

/**
 * Previously transformed traced text segments into span/tspan elements.
 *
 * Now a no-op: ZWC markers are kept in the text and JavaScript calls
 * the getTraceFromText() Java function to decode them on hover.
 */
public class TracedTextWrapperTransformer extends AbstractHtmlElementTransformer {

	public TracedTextWrapperTransformer(String zeroWidthChars) {
		// No longer used - kept for API compatibility
	}

	@Override
	public String getXPath() {
		// Match nothing - this transformer is now a no-op
		return "/nonexistent";
	}

	@Override
	public void transform(Element element) {
		// No-op: ZWC markers stay in text for JS to detect via Java callback
	}
}
