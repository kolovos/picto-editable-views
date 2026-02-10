package org.eclipse.epsilon.picto.trace;

import java.io.InputStream;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.epsilon.common.dt.util.LogUtil;
import org.eclipse.epsilon.picto.PictoView;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

/**
 * Toolbar action that allows editing the value of a traced attribute.
 */
public class EditAttributeValueAction extends TraceToolbarAction {

	@Override
	public Object run(PictoView view, Object[] parameters) {
		try {
			if (view.getEditor() instanceof IEditingDomainProvider) {
				EditingDomain editingDomain = ((IEditingDomainProvider) view.getEditor()).getEditingDomain();
				Trace trace = resolveTrace(view.getTraceMarkerManager(), parameters[0].toString());
				if (trace == null) return null;

				EObject element = (EObject) trace.getElement();
				String property = trace.getProperty();
				EAttribute attribute = element.eClass().getEAllAttributes().stream()
						.filter(a -> a.getName().equals(property)).findFirst().orElse(null);
				String currentValue = attribute != null ? String.valueOf(element.eGet(attribute)) : "";

				final String[] result = new String[1];
				final boolean[] ok = new boolean[1];
				Display.getDefault().syncExec(() -> {
					InputDialog dialog = new InputDialog(
							Display.getDefault().getActiveShell(),
							"Change value of attribute " + property,
							property,
							currentValue,
							null);
					ok[0] = dialog.open() == Window.OK;
					if (ok[0]) {
						result[0] = dialog.getValue();
					}
				});

				if (ok[0]) {
					editingDomain.getCommandStack().execute(new SetAttributeValueCommand(element, property, result[0]));
					view.render(view.getEditor()); // Refresh
				}
			}
		}
		catch (Exception ex) {
			LogUtil.log(ex);
		}
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
		return "edit";
	}

	@Override
	public String getLabel() {
		return "Edit";
	}

	@Override
	public String getTooltip() {
		return "Edit the value of this attribute in the model";
	}

	@Override
	public InputStream getIconAsStream() {
		return getClass().getResourceAsStream("edit.png");
	}

	@Override
	public String getIconSvg() {
		return "<svg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>"
			+ "<path d='M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z'/>"
			+ "</svg>";
	}

}
