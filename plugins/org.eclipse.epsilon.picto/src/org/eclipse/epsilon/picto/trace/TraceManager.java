package org.eclipse.epsilon.picto.trace;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.eol.execute.context.IEolContext;

public class TraceManager {

	protected List<Trace> traces = new ArrayList<Trace>();
	// Base-5 encoding using zero-width characters \u2060 - \u2064 as digits 0-4
	protected static final char[] ZWC_DIGITS = {'\u2060', '\u2061', '\u2062', '\u2063', '\u2064'};
	protected static final int BASE = 5;
	protected int nextTraceId = 1;

	public synchronized String getTag(IEolContext context, Object element, String property) {
		Trace trace = traces.stream().filter(t -> t.element == element && t.property.equals(property)).findFirst().orElseGet(() -> {
			Trace t = new Trace();
			t.setElement(element);
			t.setProperty(property);
			t.setContext(context);
			int id = nextTraceId++;
			t.setId(id);
			t.setTag(idToTag(id));
			traces.add(t);
			return t;
		});
		return trace.getTag();
	}

	/**
	 * Convert trace ID to base-5 ZWC tag string.
	 * Each digit (0-4) maps to a ZWC character.
	 */
	public static String idToTag(int id) {
		if (id <= 0) return String.valueOf(ZWC_DIGITS[0]);
		StringBuilder tag = new StringBuilder();
		while (id > 0) {
			tag.insert(0, ZWC_DIGITS[id % BASE]);
			id /= BASE;
		}
		return tag.toString();
	}

	/**
	 * Convert base-5 ZWC tag string back to trace ID.
	 */
	public static int tagToId(String tag) {
		int id = 0;
		for (int i = 0; i < tag.length(); i++) {
			char c = tag.charAt(i);
			int digit = -1;
			for (int d = 0; d < ZWC_DIGITS.length; d++) {
				if (ZWC_DIGITS[d] == c) {
					digit = d;
					break;
				}
			}
			if (digit < 0) return -1; // Invalid character
			id = id * BASE + digit;
		}
		return id;
	}

	/**
	 * Get trace by its ZWC tag string.
	 */
	public Trace getTrace(String tag) {
		return traces.stream().filter(t -> t.tag.equals(tag)).findFirst().orElse(null);
	}

	/**
	 * Get trace by its numeric ID.
	 */
	public Trace getTraceById(int id) {
		return traces.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
	}

	public void clear() {
		traces.clear();
		nextTraceId = 1;
	}

	/**
	 * Return all ZWC characters used for trace encoding.
	 */
	public String getZeroWidthCharacter() {
		return new String(ZWC_DIGITS);
	}
	
	
	
}
