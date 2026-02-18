package org.eclipse.epsilon.picto.trace;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Descriptor that wraps a TraceToolbarAction with its XML configuration.
 * Allows XML attributes to override Java method defaults.
 */
public class TraceToolbarActionDescriptor implements Comparable<TraceToolbarActionDescriptor> {

    protected IConfigurationElement configElement;
    protected String id;
    protected String label;
    protected String tooltip;
    protected String iconPath;
    protected int priority;
    protected TraceToolbarAction action;
    
    public TraceToolbarActionDescriptor(IConfigurationElement configElement) {
        this.configElement = configElement;
        this.id = configElement.getAttribute("id");
        this.label = configElement.getAttribute("label");
        this.tooltip = configElement.getAttribute("tooltip");
        this.iconPath = configElement.getAttribute("icon");
        String priorityStr = configElement.getAttribute("priority");
        this.priority = priorityStr != null ? Integer.parseInt(priorityStr) : 100;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        if (label != null) return label;
        return getAction().getLabel();
    }

    public String getTooltip() {
        if (tooltip != null) return tooltip;
        return getAction().getTooltip();
    }

    public int getPriority() {
        return priority;
    }

    public TraceToolbarAction getAction() {
        if (action == null) {
            try {
                action = (TraceToolbarAction) configElement.createExecutableExtension("class");
            } catch (CoreException e) {
                throw new RuntimeException("Failed to instantiate action: " + id, e);
            }
        }
        return action;
    }

    /**
     * Gets icon as InputStream. Caller is responsible for closing the stream.
     *
     * @return InputStream for the icon, or null if no icon is available
     */
    public InputStream getIconAsStream() {
        if (iconPath != null) {
            Bundle bundle = Platform.getBundle(configElement.getContributor().getName());
            if (bundle != null) {
                URL url = bundle.getEntry(iconPath);
                if (url != null) {
                    try {
                        return url.openStream();
                    } catch (IOException e) {
                        // Fall through to action method
                    }
                }
            }
        }
        return getAction().getIconAsStream();
    }

    /**
     * Gets inline SVG icon string. Takes precedence over {@link #getIconAsStream()}.
     *
     * @return SVG markup string, or null if no SVG icon
     */
    public String getIconSvg() {
        return getAction().getIconSvg();
    }

    public boolean isApplicable(Trace trace) {
        return getAction().isApplicable(trace);
    }

    @Override
    public int compareTo(TraceToolbarActionDescriptor other) {
        return Integer.compare(this.priority, other.priority);
    }
}
