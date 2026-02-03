package org.eclipse.epsilon.picto.trace;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.epsilon.picto.PictoView;
import org.eclipse.epsilon.picto.browser.PictoBrowserFunction;

/**
 * Browser function that returns a comma-separated list of applicable
 * action IDs for a given trace tag.
 */
public class GetApplicableActionsFunction implements PictoBrowserFunction {

    private final List<TraceToolbarActionDescriptor> actions;

    public GetApplicableActionsFunction(List<TraceToolbarActionDescriptor> actions) {
        this.actions = actions;
    }

    @Override
    public Object run(PictoView view, Object[] parameters) {
        if (parameters.length == 0) return "";

        String traceTag = parameters[0].toString();
        Trace trace = view.getTraceMarkerManager().getTrace(traceTag);

        if (trace == null) return "";

        String result = actions.stream()
            .filter(a -> {
                // Check if action is in allowed list (if specified)
                if (trace.getAllowedActions() != null && !trace.getAllowedActions().contains(a.getId())) {
                    return false;
                }
                return a.isApplicable(trace);
            })
            .map(TraceToolbarActionDescriptor::getId)
            .collect(Collectors.joining(","));

        return result;
    }

    @Override
    public String getName() {
        return "getApplicableTraceActions";
    }
}
