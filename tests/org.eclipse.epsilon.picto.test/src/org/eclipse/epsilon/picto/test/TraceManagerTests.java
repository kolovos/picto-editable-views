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

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.epsilon.picto.trace.Trace;
import org.eclipse.epsilon.picto.trace.TraceManager;
import org.junit.Test;

/**
 * Tests for TraceManager base-5 ZWC encoding/decoding.
 */
public class TraceManagerTests {

	@Test
	public void testIdToTagForId1() {
		// ID 1 should be digit 1 = \u2061
		String tag = new TraceManager().idToTag(1);
		assertEquals("\u2061", tag);
	}

	@Test
	public void testIdToTagForId4() {
		// ID 4 should be digit 4 = \u2064
		String tag = new TraceManager().idToTag(4);
		assertEquals("\u2064", tag);
	}

	@Test
	public void testIdToTagForId5() {
		// ID 5 = 10 in base 5 = \u2061\u2060
		String tag = new TraceManager().idToTag(5);
		assertEquals("\u2061\u2060", tag);
	}

	@Test
	public void testIdToTagForId7() {
		// ID 7 = 12 in base 5 = \u2061\u2062
		String tag = new TraceManager().idToTag(7);
		assertEquals("\u2061\u2062", tag);
	}

	@Test
	public void testIdToTagForId25() {
		// ID 25 = 100 in base 5 = \u2061\u2060\u2060
		String tag = new TraceManager().idToTag(25);
		assertEquals("\u2061\u2060\u2060", tag);
	}

	@Test
	public void testIdToTagForId100() {
		// ID 100 = 400 in base 5 = \u2064\u2060\u2060
		String tag = new TraceManager().idToTag(100);
		assertEquals("\u2064\u2060\u2060", tag);
	}

	@Test
	public void testIdToTagForId0() {
		// ID 0 should return digit 0 = \u2060
		String tag = new TraceManager().idToTag(0);
		assertEquals("\u2060", tag);
	}

	@Test
	public void testTagToIdForId1() {
		int id = new TraceManager().tagToId("\u2061");
		assertEquals(1, id);
	}

	@Test
	public void testTagToIdForId4() {
		int id = new TraceManager().tagToId("\u2064");
		assertEquals(4, id);
	}

	@Test
	public void testTagToIdForId5() {
		int id = new TraceManager().tagToId("\u2061\u2060");
		assertEquals(5, id);
	}

	@Test
	public void testTagToIdForId7() {
		int id = new TraceManager().tagToId("\u2061\u2062");
		assertEquals(7, id);
	}

	@Test
	public void testTagToIdForId25() {
		int id = new TraceManager().tagToId("\u2061\u2060\u2060");
		assertEquals(25, id);
	}

	@Test
	public void testTagToIdForId100() {
		int id = new TraceManager().tagToId("\u2064\u2060\u2060");
		assertEquals(100, id);
	}

	@Test
	public void testTagToIdForId0() {
		int id = new TraceManager().tagToId("\u2060");
		assertEquals(0, id);
	}

	@Test
	public void testTagToIdInvalidCharacter() {
		// Tag with invalid character should return -1
		int id = new TraceManager().tagToId("abc");
		assertEquals(-1, id);
	}

	@Test
	public void testTagToIdEmptyString() {
		// Empty string should return 0
		int id = new TraceManager().tagToId("");
		assertEquals(0, id);
	}

	@Test
	public void testRoundTripConversion() {
		// Test round-trip for various IDs
		int[] testIds = {1, 2, 3, 4, 5, 6, 7, 10, 15, 20, 25, 50, 100, 125, 500, 1000};
		for (int originalId : testIds) {
			String tag = new TraceManager().idToTag(originalId);
			int recoveredId = new TraceManager().tagToId(tag);
			assertEquals("Round-trip failed for ID " + originalId, originalId, recoveredId);
		}
	}

	@Test
	public void testEfficiencyImprovement() {
		// Verify that base-5 encoding is more efficient for larger IDs
		// Old approach: ID 100 would use 100 characters
		// New approach: ID 100 uses 3 characters
		String tag100 = new TraceManager().idToTag(100);
		assertEquals(3, tag100.length());

		// ID 625 = 10000 in base 5 = 5 characters (vs 625 in unary)
		String tag625 = new TraceManager().idToTag(625);
		assertEquals(5, tag625.length());
	}

	@Test
	public void testTraceWithAllowedActions() {
		TraceManager manager = new TraceManager();
		Object element = new Object();
		Collection<String> allowedActions = Arrays.asList("show", "edit");

		String tag = manager.getTag(null, element, "name", allowedActions);
		Trace trace = manager.getTrace(tag);

		assertNotNull(trace);
		assertEquals(element, trace.getElement());
		assertEquals("name", trace.getProperty());
		assertNotNull(trace.getAllowedActions());
		assertEquals(2, trace.getAllowedActions().size());
		assertTrue(trace.getAllowedActions().contains("show"));
		assertTrue(trace.getAllowedActions().contains("edit"));
	}

	@Test
	public void testTraceWithNullAllowedActions() {
		TraceManager manager = new TraceManager();
		Object element = new Object();

		String tag = manager.getTag(null, element, "name", null);
		Trace trace = manager.getTrace(tag);

		assertNotNull(trace);
		assertNull(trace.getAllowedActions());
	}

	@Test
	public void testTraceWithExplicitElement() {
		TraceManager manager = new TraceManager();
		Object element1 = new Object();
		Object element2 = new Object();
		Collection<String> allowedActions = Arrays.asList("show");

		// Create trace with element2 (the explicit element)
		String tag = manager.getTag(null, element2, "type", allowedActions);
		Trace trace = manager.getTrace(tag);

		assertNotNull(trace);
		assertEquals(element2, trace.getElement());
		assertEquals("type", trace.getProperty());
		assertEquals(1, trace.getAllowedActions().size());
		assertTrue(trace.getAllowedActions().contains("show"));
	}

	@Test
	public void testGetTagOverloadDelegates() {
		TraceManager manager = new TraceManager();
		Object element = new Object();

		// Using the 3-param version should delegate to 4-param with null actions
		String tag1 = manager.getTag(null, element, "name");
		Trace trace1 = manager.getTrace(tag1);

		assertNotNull(trace1);
		assertNull(trace1.getAllowedActions());
	}

	@Test
	public void testSameElementDifferentActionsCreatesSeparateTraces() {
		TraceManager manager = new TraceManager();
		Object element = new Object();
		Collection<String> showEdit = Arrays.asList("show", "edit");
		Collection<String> showOnly = Arrays.asList("show");

		String tag1 = manager.getTag(null, element, "name", showEdit);
		String tag2 = manager.getTag(null, element, "name", showOnly);

		// Should produce different tags since allowed actions differ
		assertNotEquals(tag1, tag2);

		Trace trace1 = manager.getTrace(tag1);
		Trace trace2 = manager.getTrace(tag2);

		assertEquals(2, trace1.getAllowedActions().size());
		assertEquals(1, trace2.getAllowedActions().size());
	}

	@Test
	public void testSameElementSameActionsReuseTrace() {
		TraceManager manager = new TraceManager();
		Object element = new Object();
		Collection<String> actions1 = Arrays.asList("show", "edit");
		Collection<String> actions2 = Arrays.asList("show", "edit");

		String tag1 = manager.getTag(null, element, "name", actions1);
		String tag2 = manager.getTag(null, element, "name", actions2);

		// Should reuse the same trace since element, property, and actions all match
		assertEquals(tag1, tag2);
	}

	@Test
	public void testSameElementNullAndNonNullActionsCreatesSeparateTraces() {
		TraceManager manager = new TraceManager();
		Object element = new Object();

		String tag1 = manager.getTag(null, element, "name", null);
		String tag2 = manager.getTag(null, element, "name", Arrays.asList("show"));

		// Null actions vs non-null actions should create separate traces
		assertNotEquals(tag1, tag2);

		Trace trace1 = manager.getTrace(tag1);
		Trace trace2 = manager.getTrace(tag2);

		assertNull(trace1.getAllowedActions());
		assertNotNull(trace2.getAllowedActions());
	}
}
