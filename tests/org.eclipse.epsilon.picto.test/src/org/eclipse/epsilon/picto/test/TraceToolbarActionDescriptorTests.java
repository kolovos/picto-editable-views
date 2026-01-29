/*********************************************************************
* Copyright (c) 2020 The University of York.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.picto.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.epsilon.picto.PictoView;
import org.eclipse.epsilon.picto.trace.Trace;
import org.eclipse.epsilon.picto.trace.TraceToolbarAction;
import org.eclipse.epsilon.picto.trace.TraceToolbarActionDescriptor;
import org.junit.Test;

/**
 * Tests for TraceToolbarActionDescriptor.
 */
public class TraceToolbarActionDescriptorTests {

	/**
	 * Test action implementation.
	 */
	public static class TestAction extends TraceToolbarAction {
		@Override
		public String getId() {
			return "testAction";
		}

		@Override
		public String getLabel() {
			return "Test Label";
		}

		@Override
		public String getTooltip() {
			return "Test Tooltip";
		}

		@Override
		public boolean isApplicable(Trace trace) {
			return trace != null && "editable".equals(trace.getProperty());
		}

		@Override
		public Object run(PictoView view, Object[] parameters) {
			return "test executed";
		}
	}

	/**
	 * Action that is never applicable.
	 */
	public static class NeverApplicableAction extends TraceToolbarAction {
		@Override
		public String getId() {
			return "neverApplicable";
		}

		@Override
		public boolean isApplicable(Trace trace) {
			return false;
		}

		@Override
		public Object run(PictoView view, Object[] parameters) {
			return null;
		}
	}

	/**
	 * Mock implementation of IConfigurationElement for testing.
	 */
	private static class MockConfigurationElement implements IConfigurationElement {
		private final Map<String, String> attributes = new HashMap<>();
		private final Class<?> actionClass;

		public MockConfigurationElement(Class<?> actionClass) {
			this.actionClass = actionClass;
		}

		public MockConfigurationElement setAttribute(String name, String value) {
			attributes.put(name, value);
			return this;
		}

		@Override
		public String getAttribute(String name) throws InvalidRegistryObjectException {
			return attributes.get(name);
		}

		@Override
		public Object createExecutableExtension(String propertyName) throws CoreException {
			try {
				return actionClass.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		// Required interface methods - not used in tests
		@Override
		public String getAttributeAsIs(String name) throws InvalidRegistryObjectException {
			return attributes.get(name);
		}

		@Override
		public String[] getAttributeNames() throws InvalidRegistryObjectException {
			return attributes.keySet().toArray(new String[0]);
		}

		@Override
		public IConfigurationElement[] getChildren() throws InvalidRegistryObjectException {
			return new IConfigurationElement[0];
		}

		@Override
		public IConfigurationElement[] getChildren(String name) throws InvalidRegistryObjectException {
			return new IConfigurationElement[0];
		}

		@Override
		public IContributor getContributor() throws InvalidRegistryObjectException {
			return new IContributor() {
				@Override
				public String getName() {
					return "test.plugin";
				}
			};
		}

		@Override
		public IExtension getDeclaringExtension() throws InvalidRegistryObjectException {
			return null;
		}

		@Override
		public String getName() throws InvalidRegistryObjectException {
			return "traceToolbarAction";
		}

		@Override
		public Object getParent() throws InvalidRegistryObjectException {
			return null;
		}

		@Override
		public String getValue() throws InvalidRegistryObjectException {
			return null;
		}

		@Override
		public String getValueAsIs() throws InvalidRegistryObjectException {
			return null;
		}

		@Override
		public String getNamespaceIdentifier() throws InvalidRegistryObjectException {
			return "org.eclipse.epsilon.picto";
		}

		@Override
		public boolean isValid() {
			return true;
		}

		@Override
		public String getAttribute(String attrName, String locale) throws InvalidRegistryObjectException {
			return attributes.get(attrName);
		}

		@Override
		public String getValue(String locale) throws InvalidRegistryObjectException {
			return null;
		}

		@Override
		public int getHandleId() {
			return 0;
		}

		@Override
		public String getNamespace() throws InvalidRegistryObjectException {
			return "org.eclipse.epsilon.picto";
		}
	}

	@Test
	public void testGetIdFromAttribute() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "myAction")
			.setAttribute("class", "org.eclipse.epsilon.picto.test.TraceToolbarActionDescriptorTests$TestAction");

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);
		assertEquals("myAction", descriptor.getId());
	}

	@Test
	public void testGetLabelFromXmlOverridesJava() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "test")
			.setAttribute("label", "XML Label");

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);
		assertEquals("XML Label", descriptor.getLabel());
	}

	@Test
	public void testGetLabelFallsBackToJavaMethod() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "test");
		// No label attribute set

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);
		assertEquals("Test Label", descriptor.getLabel());
	}

	@Test
	public void testGetTooltipFromXmlOverridesJava() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "test")
			.setAttribute("tooltip", "XML Tooltip");

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);
		assertEquals("XML Tooltip", descriptor.getTooltip());
	}

	@Test
	public void testGetTooltipFallsBackToJavaMethod() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "test");
		// No tooltip attribute set

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);
		assertEquals("Test Tooltip", descriptor.getTooltip());
	}

	@Test
	public void testDefaultPriorityIs100() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "test");
		// No priority attribute set

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);
		assertEquals(100, descriptor.getPriority());
	}

	@Test
	public void testCustomPriorityFromAttribute() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "test")
			.setAttribute("priority", "50");

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);
		assertEquals(50, descriptor.getPriority());
	}

	@Test
	public void testGetActionCreatesInstance() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "test");

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);
		TraceToolbarAction action = descriptor.getAction();

		assertNotNull(action);
		assertTrue(action instanceof TestAction);
	}

	@Test
	public void testGetActionReturnsSameInstance() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "test");

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);
		TraceToolbarAction action1 = descriptor.getAction();
		TraceToolbarAction action2 = descriptor.getAction();

		assertSame(action1, action2);
	}

	@Test
	public void testIsApplicableDelegatesToAction() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "test");

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);

		Trace editableTrace = new Trace();
		editableTrace.setProperty("editable");

		Trace readonlyTrace = new Trace();
		readonlyTrace.setProperty("readonly");

		assertTrue(descriptor.isApplicable(editableTrace));
		assertFalse(descriptor.isApplicable(readonlyTrace));
		assertFalse(descriptor.isApplicable(null));
	}

	@Test
	public void testCompareToSortsByPriority() {
		MockConfigurationElement config1 = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "low")
			.setAttribute("priority", "10");

		MockConfigurationElement config2 = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "high")
			.setAttribute("priority", "200");

		MockConfigurationElement config3 = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "default");
		// priority defaults to 100

		TraceToolbarActionDescriptor desc1 = new TraceToolbarActionDescriptor(config1);
		TraceToolbarActionDescriptor desc2 = new TraceToolbarActionDescriptor(config2);
		TraceToolbarActionDescriptor desc3 = new TraceToolbarActionDescriptor(config3);

		assertTrue(desc1.compareTo(desc2) < 0); // 10 < 200
		assertTrue(desc1.compareTo(desc3) < 0); // 10 < 100
		assertTrue(desc3.compareTo(desc2) < 0); // 100 < 200
		assertTrue(desc2.compareTo(desc1) > 0); // 200 > 10
		assertEquals(0, desc1.compareTo(desc1)); // same priority
	}

	@Test
	public void testSortingDescriptorsByPriority() {
		MockConfigurationElement config1 = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "third")
			.setAttribute("priority", "300");

		MockConfigurationElement config2 = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "first")
			.setAttribute("priority", "10");

		MockConfigurationElement config3 = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "second")
			.setAttribute("priority", "100");

		List<TraceToolbarActionDescriptor> descriptors = new ArrayList<>();
		descriptors.add(new TraceToolbarActionDescriptor(config1));
		descriptors.add(new TraceToolbarActionDescriptor(config2));
		descriptors.add(new TraceToolbarActionDescriptor(config3));

		Collections.sort(descriptors);

		assertEquals("first", descriptors.get(0).getId());
		assertEquals("second", descriptors.get(1).getId());
		assertEquals("third", descriptors.get(2).getId());
	}

	@Test
	public void testFilteringByApplicability() {
		MockConfigurationElement configApplicable = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "applicable");

		MockConfigurationElement configNeverApplicable = new MockConfigurationElement(NeverApplicableAction.class)
			.setAttribute("id", "never");

		List<TraceToolbarActionDescriptor> descriptors = new ArrayList<>();
		descriptors.add(new TraceToolbarActionDescriptor(configApplicable));
		descriptors.add(new TraceToolbarActionDescriptor(configNeverApplicable));

		Trace trace = new Trace();
		trace.setProperty("editable");

		List<String> applicableIds = new ArrayList<>();
		for (TraceToolbarActionDescriptor desc : descriptors) {
			if (desc.isApplicable(trace)) {
				applicableIds.add(desc.getId());
			}
		}

		assertEquals(1, applicableIds.size());
		assertTrue(applicableIds.contains("applicable"));
		assertFalse(applicableIds.contains("never"));
	}

	@Test
	public void testIconPathAttribute() {
		MockConfigurationElement config = new MockConfigurationElement(TestAction.class)
			.setAttribute("id", "test")
			.setAttribute("icon", "icons/test.png");

		TraceToolbarActionDescriptor descriptor = new TraceToolbarActionDescriptor(config);
		// getIconAsStream will try to load from bundle, which returns null in test
		// but won't throw an exception
		assertNull(descriptor.getIconAsStream());
	}
}
