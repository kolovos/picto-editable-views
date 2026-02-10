package org.eclipse.epsilon.picto.trace;

import java.io.InputStream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.epsilon.picto.PictoView;

/**
 * Toolbar action that shows the traced element in the editor.
 */
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

	@Override
	public String getLabel() {
		return "Show";
	}

	@Override
	public String getTooltip() {
		return "Show element in editor";
	}

	@Override
	public InputStream getIconAsStream() {
		return getClass().getResourceAsStream("show.png");
	}

	@Override
	public String getIconSvg() {
		return "<svg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>"
			+ "<circle cx='11' cy='11' r='8'/>"
			+ "<line x1='21' y1='21' x2='16.65' y2='16.65'/>"
			+ "</svg>";
	}

}
