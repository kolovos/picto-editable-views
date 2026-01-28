package org.eclipse.epsilon.picto.trace;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.eol.execute.context.IEolContext;

public class TraceManager {

	protected List<Trace> traces = new ArrayList<Trace>();
	// More zero-width characters to consider \u2061 - \u2064
	protected String zeroWidthChar = "\u2060";
	protected int nextTraceId = 1;

	public synchronized String getTag(IEolContext context, Object element, String property) {
		Trace trace = traces.stream().filter(t -> t.element == element && t.property.equals(property)).findFirst().orElseGet(() -> {
			Trace t = new Trace();
			t.setElement(element);
			t.setProperty(property);
			t.setContext(context);
			int id = nextTraceId++;
			t.setId(id);
			String tag = "";
			for (int i = 0; i < id; i++) tag += zeroWidthChar;
			t.setTag(tag);
			traces.add(t);
			return t;
		});
		return trace.getTag();
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

	public String getZeroWidthCharacter() {
		return zeroWidthChar;
	}
	
	
	
}
