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

        String traceRef = parameters[0].toString();
        Trace trace = resolveTrace(view.getTraceMarkerManager(), traceRef);

        if (trace == null) return "";

        String result = actions.stream()
            .filter(a -> a.isApplicable(trace))
            .map(TraceToolbarActionDescriptor::getId)
            .collect(Collectors.joining(","));

        return result;
    }

    /**
     * Resolve trace from either numeric ID or ZWC tag string.
     */
    private Trace resolveTrace(TraceManager manager, String traceRef) {
        try {
            int id = Integer.parseInt(traceRef);
            Trace trace = manager.getTraceById(id);
            if (trace != null) return trace;
        } catch (NumberFormatException e) {
            // Not a number, try as ZWC string
        }
        return manager.getTrace(traceRef);
    }

    @Override
    public String getName() {
        return "getApplicableTraceActions";
    }
}
