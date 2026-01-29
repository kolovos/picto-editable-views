/*********************************************************************
* Copyright (c) 2008 The University of York.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.picto.trace;

import org.eclipse.epsilon.picto.PictoView;
import org.eclipse.epsilon.picto.browser.PictoBrowserFunction;

/**
 * Browser function that extracts a trace ID from text containing ZWC markers.
 * Called by JavaScript to determine if text is traceable and get the trace ID.
 *
 * Supports two formats:
 * - Bracketed: [tag]text[tag] where the same ZWC sequence opens and closes
 * - Suffix: text[tag] where ZWC sequence is at the end (legacy format)
 */
public class GetTraceFromTextFunction implements PictoBrowserFunction {

    @Override
    public Object run(PictoView view, Object[] parameters) {
        if (parameters.length == 0 || parameters[0] == null) {
            return "";
        }

        String text = parameters[0].toString();
        String zwcChars = view.getTraceMarkerManager().getZeroWidthCharacter();

        return getTraceFromText(text, zwcChars);
    }

    /**
     * Extract trace ID from text containing ZWC markers.
     * This static method can be used for testing without Eclipse dependencies.
     *
     * @param text The text to search for traces
     * @param zwcChars The ZWC characters used for encoding (e.g., "\u2060\u2061\u2062\u2063\u2064")
     * @return The trace ID as a string, or empty string if no trace found
     */
    public static String getTraceFromText(String text, String zwcChars) {
        if (zwcChars == null || zwcChars.isEmpty() || text == null || text.isEmpty()) {
            return "";
        }

        // Try bracketed format first: [tag]text[tag]
        Integer traceId = parseBracketedTrace(text, zwcChars);
        if (traceId != null) {
            return String.valueOf(traceId);
        }

        // Fall back to suffix format: text[tag]
        traceId = parseSuffixTrace(text, zwcChars);
        if (traceId != null) {
            return String.valueOf(traceId);
        }

        return "";
    }

    /**
     * Parse bracketed format: [tag]text[tag]
     * Returns the first trace ID found, or null if none.
     */
    private static Integer parseBracketedTrace(String text, String zwcChars) {
        Integer openTagId = null;
        int i = 0;

        while (i < text.length()) {
            if (isZwc(text.charAt(i), zwcChars)) {
                int seqStart = i;
                while (i < text.length() && isZwc(text.charAt(i), zwcChars)) {
                    i++;
                }
                String zwcSeq = text.substring(seqStart, i);
                int traceId = decodeZwcSequence(zwcSeq, zwcChars);

                if (openTagId == null) {
                    openTagId = traceId;
                } else if (traceId == openTagId) {
                    // Found matching close tag
                    return traceId;
                }
                // Different tag while one is open - continue looking
            } else {
                i++;
            }
        }

        return null;
    }

    /**
     * Parse suffix format: text[tag]
     * Returns the trace ID from trailing ZWC characters, or null if none.
     */
    private static Integer parseSuffixTrace(String text, String zwcChars) {
        int position = text.length() - 1;
        StringBuilder suffix = new StringBuilder();

        while (position >= 0 && isZwc(text.charAt(position), zwcChars)) {
            suffix.insert(0, text.charAt(position));
            position--;
        }

        if (suffix.length() > 0) {
            int traceId = decodeZwcSequence(suffix.toString(), zwcChars);
            if (traceId >= 0) {
                return traceId;
            }
        }

        return null;
    }

    private static boolean isZwc(char c, String zwcChars) {
        return zwcChars.indexOf(c) >= 0;
    }

    private static int decodeZwcSequence(String seq, String zwcChars) {
        int id = 0;
        int base = zwcChars.length();
        for (int i = 0; i < seq.length(); i++) {
            int digit = zwcChars.indexOf(seq.charAt(i));
            if (digit < 0) return -1;
            id = id * base + digit;
        }
        return id;
    }

    @Override
    public String getName() {
        return "getTraceFromText";
    }
}
