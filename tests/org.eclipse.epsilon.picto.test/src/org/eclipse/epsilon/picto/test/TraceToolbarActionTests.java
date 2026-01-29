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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.epsilon.picto.PictoView;
import org.eclipse.epsilon.picto.trace.Trace;
import org.eclipse.epsilon.picto.trace.TraceToolbarAction;
import org.junit.Test;

/**
 * Tests for TraceToolbarAction base class behavior.
 */
public class TraceToolbarActionTests {

	/**
	 * Minimal concrete implementation for testing base class defaults.
	 */
	private static class MinimalAction extends TraceToolbarAction {
		@Override
		public String getId() {
			return "minimal";
		}

		@Override
		public Object run(PictoView view, Object[] parameters) {
			return null;
		}
	}

	/**
	 * Custom implementation that overrides all methods.
	 */
	private static class CustomAction extends TraceToolbarAction {
		private final String id;
		private final String label;
		private final String tooltip;
		private final boolean applicable;

		public CustomAction(String id, String label, String tooltip, boolean applicable) {
			this.id = id;
			this.label = label;
			this.tooltip = tooltip;
			this.applicable = applicable;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public String getTooltip() {
			return tooltip;
		}

		@Override
		public InputStream getIconAsStream() {
			return new ByteArrayInputStream(new byte[]{1, 2, 3});
		}

		@Override
		public boolean isApplicable(Trace trace) {
			return applicable;
		}

		@Override
		public Object run(PictoView view, Object[] parameters) {
			return "executed";
		}
	}

	@Test
	public void testGetNamePrefixesWithPictoToolbar() {
		MinimalAction action = new MinimalAction();
		assertEquals("picto_toolbar_minimal", action.getName());
	}

	@Test
	public void testGetNameWithCustomId() {
		CustomAction action = new CustomAction("myAction", "My Action", "Do something", true);
		assertEquals("picto_toolbar_myAction", action.getName());
	}

	@Test
	public void testDefaultLabelReturnsId() {
		MinimalAction action = new MinimalAction();
		assertEquals("minimal", action.getLabel());
	}

	@Test
	public void testCustomLabelOverridesDefault() {
		CustomAction action = new CustomAction("test", "Custom Label", "Tooltip", true);
		assertEquals("Custom Label", action.getLabel());
	}

	@Test
	public void testDefaultTooltipReturnsLabel() {
		MinimalAction action = new MinimalAction();
		// Default tooltip returns getLabel(), which returns getId()
		assertEquals("minimal", action.getTooltip());
	}

	@Test
	public void testCustomTooltipOverridesDefault() {
		CustomAction action = new CustomAction("test", "Label", "Custom Tooltip", true);
		assertEquals("Custom Tooltip", action.getTooltip());
	}

	@Test
	public void testDefaultIconReturnsNull() {
		MinimalAction action = new MinimalAction();
		assertNull(action.getIconAsStream());
	}

	@Test
	public void testCustomIconReturnsStream() {
		CustomAction action = new CustomAction("test", "Label", "Tooltip", true);
		InputStream stream = action.getIconAsStream();
		assertNotNull(stream);
	}

	@Test
	public void testDefaultIsApplicableReturnsTrue() {
		MinimalAction action = new MinimalAction();
		Trace trace = new Trace();
		assertTrue(action.isApplicable(trace));
	}

	@Test
	public void testDefaultIsApplicableReturnsTrueForNullTrace() {
		MinimalAction action = new MinimalAction();
		assertTrue(action.isApplicable(null));
	}

	@Test
	public void testCustomIsApplicableCanReturnFalse() {
		CustomAction action = new CustomAction("test", "Label", "Tooltip", false);
		Trace trace = new Trace();
		assertFalse(action.isApplicable(trace));
	}

	@Test
	public void testCustomIsApplicableCanReturnTrue() {
		CustomAction action = new CustomAction("test", "Label", "Tooltip", true);
		Trace trace = new Trace();
		assertTrue(action.isApplicable(trace));
	}

	/**
	 * Action that checks trace properties for applicability.
	 */
	private static class PropertyCheckingAction extends TraceToolbarAction {
		private final String requiredProperty;

		public PropertyCheckingAction(String requiredProperty) {
			this.requiredProperty = requiredProperty;
		}

		@Override
		public String getId() {
			return "propertyChecker";
		}

		@Override
		public boolean isApplicable(Trace trace) {
			if (trace == null) return false;
			return requiredProperty.equals(trace.getProperty());
		}

		@Override
		public Object run(PictoView view, Object[] parameters) {
			return null;
		}
	}

	@Test
	public void testIsApplicableCanCheckTraceProperty() {
		PropertyCheckingAction action = new PropertyCheckingAction("name");

		Trace traceWithName = new Trace();
		traceWithName.setProperty("name");

		Trace traceWithAge = new Trace();
		traceWithAge.setProperty("age");

		assertTrue(action.isApplicable(traceWithName));
		assertFalse(action.isApplicable(traceWithAge));
		assertFalse(action.isApplicable(null));
	}

	@Test
	public void testMultipleActionsHaveDifferentNames() {
		TraceToolbarAction action1 = new CustomAction("edit", "Edit", "Edit value", true);
		TraceToolbarAction action2 = new CustomAction("show", "Show", "Show element", true);

		assertFalse(action1.getName().equals(action2.getName()));
		assertEquals("picto_toolbar_edit", action1.getName());
		assertEquals("picto_toolbar_show", action2.getName());
	}
}
