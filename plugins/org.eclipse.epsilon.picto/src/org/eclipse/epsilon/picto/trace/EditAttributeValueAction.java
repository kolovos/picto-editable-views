package org.eclipse.epsilon.picto.trace;

import java.io.InputStream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.epsilon.common.dt.util.LogUtil;
import org.eclipse.epsilon.eol.dt.userinput.JFaceUserInput;
import org.eclipse.epsilon.picto.PictoView;

/**
 * Toolbar action that allows editing the value of a traced attribute.
 */
public class EditAttributeValueAction extends TraceToolbarAction {

	@Override
	public Object run(PictoView view, Object[] parameters) {
		try {
			if (view.getEditor() instanceof IEditingDomainProvider) {
				EditingDomain editingDomain = ((IEditingDomainProvider) view.getEditor()).getEditingDomain();
				Trace trace = view.getTraceMarkerManager().getTrace(parameters[0].toString());
				String value = new JFaceUserInput(trace.getContext().getPrettyPrinterManager()).prompt(trace.getProperty());
				editingDomain.getCommandStack().execute(new SetAttributeValueCommand((EObject) trace.getElement(), trace.getProperty(), value));
				view.render(view.getEditor()); // Refresh
			}
		}
		catch (Exception ex) {
			LogUtil.log(ex);
		}
		return null;
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
		return "Edit attribute value";
	}

	@Override
	public InputStream getIconAsStream() {
		return getClass().getResourceAsStream("edit.png");
	}

	@Override
	public boolean isApplicable(Trace trace) {
		return true;
	}
}
