package org.eclipse.epsilon.picto.trace;

import java.io.InputStream;

import org.eclipse.epsilon.picto.browser.PictoBrowserFunction;

/**
 * Abstract base class for trace toolbar actions.
 * Subclasses implement specific actions that appear in the toolbar
 * when hovering over traced elements.
 */
public abstract class TraceToolbarAction implements PictoBrowserFunction {

	public abstract String getId();

	@Override
	public String getName() {
		return "picto_toolbar_" + getId();
	}

	/**
	 * Returns the label for this action.
	 * Override to provide a custom label.
	 *
	 * @return the action label, defaults to the action ID
	 */
	public String getLabel() {
		return getId();
	}

	/**
	 * Returns the tooltip for this action.
	 * Override to provide a custom tooltip.
	 *
	 * @return the tooltip text, defaults to the label
	 */
	public String getTooltip() {
		return getLabel();
	}

	/**
	 * Returns the icon for this action as an InputStream.
	 * Caller is responsible for closing the stream.
	 * Override to provide a custom icon.
	 *
	 * @return InputStream for the icon, or null if no icon
	 */
	public InputStream getIconAsStream() {
		return null;
	}

	/**
	 * Determines if this action is applicable for the given trace.
	 * Override to conditionally show/hide the action based on the traced element.
	 *
	 * @param trace the trace to check
	 * @return true if the action should be shown, false otherwise
	 */
	public boolean isApplicable(Trace trace) {
		return true;
	}
}
