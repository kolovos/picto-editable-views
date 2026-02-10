package org.eclipse.epsilon.picto.trace;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.epsilon.egl.execute.operations.EglOperationFactory;
import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.dom.NameExpression;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.introspection.recording.IPropertyAccess;
import org.eclipse.epsilon.eol.execute.introspection.recording.PropertyAccessExecutionListener;
import org.eclipse.epsilon.eol.execute.introspection.recording.PropertyAccessRecorder;
import org.eclipse.epsilon.eol.execute.operations.AbstractOperation;
import org.eclipse.epsilon.picto.PictoView;

public class TraceOperationFactory extends EglOperationFactory {

	public TraceOperationFactory(PictoView pictoView) {
		super();
		operationCache.put("trace", new AbstractOperation() {

			@Override
			public Object execute(Object target, NameExpression operationNameExpression, List<Parameter> iterators,
					List<Expression> expressions, IEolContext context) throws EolRuntimeException {

				int paramCount = expressions.size();

				// First parameter is always the value expression
				Expression valueExpression = expressions.get(0);

				PropertyAccessRecorder recorder = new PropertyAccessRecorder();
				recorder.startRecording();
				PropertyAccessExecutionListener listener = new PropertyAccessExecutionListener(recorder);
				context.getExecutorFactory().addExecutionListener(listener);

				Object result = context.getExecutorFactory().execute(valueExpression, context);

				context.getExecutorFactory().removeExecutionListener(listener);

				// Get the recorded property access if one was captured.
				// The recorder may be empty when the value expression is a
				// pre-computed variable rather than a property access (e.g.
				// trace(localVar, element, actions)).
				IPropertyAccess propertyAccess = null;
				if (!recorder.getPropertyAccesses().unique().isEmpty()) {
					propertyAccess = recorder.getPropertyAccesses().unique().iterator().next();
				}

				// Determine element, property, and allowed actions based on parameter count
				Object element;
				String property;
				Collection<String> allowedActions = null;

				if (paramCount >= 3) {
					// trace(value, element, actions) - use explicit element, specific actions
					element = context.getExecutorFactory().execute(expressions.get(1), context);
					property = propertyAccess != null ? propertyAccess.getPropertyName() : null;
					allowedActions = extractAllowedActions(context.getExecutorFactory().execute(expressions.get(2), context));
				} else if (paramCount == 2) {
					// trace(value, actions) - use recorded element/property, specific actions
					if (propertyAccess == null) {
						throw new EolRuntimeException("trace(value, actions) requires the value expression to include a property access (e.g. trace(obj.name, actions))");
					}
					element = propertyAccess.getModelElement();
					property = propertyAccess.getPropertyName();
					allowedActions = extractAllowedActions(context.getExecutorFactory().execute(expressions.get(1), context));
				} else {
					// trace(value) - use recorded element/property, all actions
					if (propertyAccess == null) {
						throw new EolRuntimeException("trace(value) requires the value expression to include a property access (e.g. trace(obj.name))");
					}
					element = propertyAccess.getModelElement();
					property = propertyAccess.getPropertyName();
				}

				// Surround text with tags to support multiple traced strings in the same HTML element
				String tag = pictoView.getTraceMarkerManager().getTag(context, element, property, allowedActions);
				return tag + result + tag;
			}

			@SuppressWarnings("unchecked")
			private Collection<String> extractAllowedActions(Object actionsParam) {
				if (actionsParam instanceof Collection) {
					return ((Collection<?>) actionsParam).stream()
						.map(Object::toString)
						.collect(Collectors.toList());
				}
				return null;
			}
		});
	}

}
