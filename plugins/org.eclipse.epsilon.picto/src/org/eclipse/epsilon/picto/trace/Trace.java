package org.eclipse.epsilon.picto.trace;

import java.util.Collection;

import org.eclipse.epsilon.eol.execute.context.IEolContext;

public class Trace {

		protected int id;
		protected Object element;
		protected String property;
		protected IEolContext context;
		protected String tag;
		protected Collection<String> allowedActions;
		
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Object getElement() {
			return element;
		}
		
		public void setElement(Object element) {
			this.element = element;
		}
		
		public String getProperty() {
			return property;
		}
		
		public void setProperty(String property) {
			this.property = property;
		}
		
		public String getTag() {
			return tag;
		}
		
		public void setTag(String tag) {
			this.tag = tag;
		}
		
		public void setContext(IEolContext context) {
			this.context = context;
		}
		
		public IEolContext getContext() {
			return context;
		}

		public Collection<String> getAllowedActions() {
			return allowedActions;
		}

		public void setAllowedActions(Collection<String> allowedActions) {
			this.allowedActions = allowedActions;
		}
	}