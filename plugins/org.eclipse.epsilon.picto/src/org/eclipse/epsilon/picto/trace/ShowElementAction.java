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
		Trace trace = view.getTraceMarkerManager().getTrace(parameters[0].toString());
		// TODO: Revisit assumption that the element is an EObject
		EObject eObject = (EObject) trace.getElement();
		view.getSource().showElement(eObject.eResource().getURIFragment(eObject), eObject.eResource().getURI().toString(), view.getEditor());
		return null;
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
	public boolean isApplicable(Trace trace) {
		return true;
	}
}
