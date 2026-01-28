package org.eclipse.epsilon.picto.trace;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.epsilon.picto.PictoView;

public class ShowElementAction extends TraceToolbarAction {

	@Override
	public Object run(PictoView view, Object[] parameters) {
		Trace trace = resolveTrace(view.getTraceMarkerManager(), parameters[0].toString());
		if (trace == null) return null;
		// TODO: Revisit assumption that the element is an EObject
		EObject eObject = (EObject) trace.getElement();
		view.getSource().showElement(eObject.eResource().getURIFragment(eObject), eObject.eResource().getURI().toString(), view.getEditor());
		return null;
	}

	/**
	 * Resolve trace from either numeric ID or ZWC tag string.
	 */
	private Trace resolveTrace(TraceManager manager, String traceRef) {
		// Try as numeric ID first (new approach)
		try {
			int id = Integer.parseInt(traceRef);
			Trace trace = manager.getTraceById(id);
			if (trace != null) return trace;
		} catch (NumberFormatException e) {
			// Not a number, try as ZWC string
		}
		// Fallback: treat as ZWC tag string (legacy approach)
		return manager.getTrace(traceRef);
	}

	@Override
	public String getId() {
		return "show";
	}

}
