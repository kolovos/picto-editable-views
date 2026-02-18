package org.eclipse.epsilon.picto.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * Manages trace toolbar action extensions.
 * Loads and caches action descriptors from the extension point.
 */
public class TraceToolbarActionExtensionPointManager {

    protected String EXTENSION_POINT_ID = "org.eclipse.epsilon.picto.traceToolbarAction";
    protected List<TraceToolbarActionDescriptor> descriptors;

    /**
     * Gets all registered trace toolbar action extensions, sorted by priority.
     *
     * @return List of action descriptors
     */
    public List<TraceToolbarActionDescriptor> getExtensions() {
        if (descriptors == null) {
            descriptors = new ArrayList<>();
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            if (registry != null) {
                IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID);

                for (IConfigurationElement element : elements) {
                    if ("traceToolbarAction".equals(element.getName())) {
                        descriptors.add(new TraceToolbarActionDescriptor(element));
                    }
                }

                // Sort by priority
                Collections.sort(descriptors);
            }
        }
        return descriptors;
    }

    /**
     * Clears the cached extensions, forcing a reload on next access.
     */
    public void clearCache() {
        descriptors = null;
    }
}
