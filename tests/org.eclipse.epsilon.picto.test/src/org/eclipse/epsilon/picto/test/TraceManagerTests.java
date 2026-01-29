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

import org.eclipse.epsilon.picto.trace.TraceManager;
import org.junit.Test;

/**
 * Tests for TraceManager base-5 ZWC encoding/decoding.
 */
public class TraceManagerTests {

	@Test
	public void testIdToTagForId1() {
		// ID 1 should be digit 1 = \u2061
		String tag = TraceManager.idToTag(1);
		assertEquals("\u2061", tag);
	}

	@Test
	public void testIdToTagForId4() {
		// ID 4 should be digit 4 = \u2064
		String tag = TraceManager.idToTag(4);
		assertEquals("\u2064", tag);
	}

	@Test
	public void testIdToTagForId5() {
		// ID 5 = 10 in base 5 = \u2061\u2060
		String tag = TraceManager.idToTag(5);
		assertEquals("\u2061\u2060", tag);
	}

	@Test
	public void testIdToTagForId7() {
		// ID 7 = 12 in base 5 = \u2061\u2062
		String tag = TraceManager.idToTag(7);
		assertEquals("\u2061\u2062", tag);
	}

	@Test
	public void testIdToTagForId25() {
		// ID 25 = 100 in base 5 = \u2061\u2060\u2060
		String tag = TraceManager.idToTag(25);
		assertEquals("\u2061\u2060\u2060", tag);
	}

	@Test
	public void testIdToTagForId100() {
		// ID 100 = 400 in base 5 = \u2064\u2060\u2060
		String tag = TraceManager.idToTag(100);
		assertEquals("\u2064\u2060\u2060", tag);
	}

	@Test
	public void testIdToTagForId0() {
		// ID 0 should return digit 0 = \u2060
		String tag = TraceManager.idToTag(0);
		assertEquals("\u2060", tag);
	}

	@Test
	public void testTagToIdForId1() {
		int id = TraceManager.tagToId("\u2061");
		assertEquals(1, id);
	}

	@Test
	public void testTagToIdForId4() {
		int id = TraceManager.tagToId("\u2064");
		assertEquals(4, id);
	}

	@Test
	public void testTagToIdForId5() {
		int id = TraceManager.tagToId("\u2061\u2060");
		assertEquals(5, id);
	}

	@Test
	public void testTagToIdForId7() {
		int id = TraceManager.tagToId("\u2061\u2062");
		assertEquals(7, id);
	}

	@Test
	public void testTagToIdForId25() {
		int id = TraceManager.tagToId("\u2061\u2060\u2060");
		assertEquals(25, id);
	}

	@Test
	public void testTagToIdForId100() {
		int id = TraceManager.tagToId("\u2064\u2060\u2060");
		assertEquals(100, id);
	}

	@Test
	public void testTagToIdForId0() {
		int id = TraceManager.tagToId("\u2060");
		assertEquals(0, id);
	}

	@Test
	public void testTagToIdInvalidCharacter() {
		// Tag with invalid character should return -1
		int id = TraceManager.tagToId("abc");
		assertEquals(-1, id);
	}

	@Test
	public void testTagToIdEmptyString() {
		// Empty string should return 0
		int id = TraceManager.tagToId("");
		assertEquals(0, id);
	}

	@Test
	public void testRoundTripConversion() {
		// Test round-trip for various IDs
		int[] testIds = {1, 2, 3, 4, 5, 6, 7, 10, 15, 20, 25, 50, 100, 125, 500, 1000};
		for (int originalId : testIds) {
			String tag = TraceManager.idToTag(originalId);
			int recoveredId = TraceManager.tagToId(tag);
			assertEquals("Round-trip failed for ID " + originalId, originalId, recoveredId);
		}
	}

	@Test
	public void testGetZeroWidthCharacterReturnsAllFive() {
		TraceManager manager = new TraceManager();
		String zwcChars = manager.getZeroWidthCharacter();
		assertEquals(5, zwcChars.length());
		assertEquals('\u2060', zwcChars.charAt(0));
		assertEquals('\u2061', zwcChars.charAt(1));
		assertEquals('\u2062', zwcChars.charAt(2));
		assertEquals('\u2063', zwcChars.charAt(3));
		assertEquals('\u2064', zwcChars.charAt(4));
	}

	@Test
	public void testEfficiencyImprovement() {
		// Verify that base-5 encoding is more efficient for larger IDs
		// Old approach: ID 100 would use 100 characters
		// New approach: ID 100 uses 3 characters
		String tag100 = TraceManager.idToTag(100);
		assertEquals(3, tag100.length());

		// ID 625 = 10000 in base 5 = 5 characters (vs 625 in unary)
		String tag625 = TraceManager.idToTag(625);
		assertEquals(5, tag625.length());
	}
}
