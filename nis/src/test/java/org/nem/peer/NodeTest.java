package org.nem.peer;

import static org.junit.Assert.*;

import org.junit.Test;

public class NodeTest {

	@Test
	public void testNodeNULL() {
		try {
			new Node(null);
			fail("Creation with NULL possible.");
		} catch (IllegalArgumentException ex) {
			// As expected
		}
	}

	@Test
	public void testNodeEmpty() {
		try {
			new Node("");
			fail("Creation with empty string possible.");
		} catch (IllegalArgumentException ex) {
			// As expected
		}
	}

	@Test
	public void testNodeURL1() {
		String urlStr = "localhost sine";
		try {
			new Node(urlStr);
			fail("Creation with <" + urlStr + "> was possible.");
		} catch (IllegalArgumentException ex) {
			// As expected
		}
	}

	@Test
	public void testNodeURL2() {
		String urlStr = "http://127.0.0.1";
		try {
			new Node(urlStr);
			fail("Creation with <" + urlStr + "> was possible.");
		} catch (IllegalArgumentException ex) {
			// As expected
		}
	}

	@Test
	public void testNodeURL3() {
		String urlStr = "127.0.0.1";

		Node node = new Node(urlStr);
		
		assertNotNull(node);
		assertNotNull(node.getAddress());
		assertEquals(node.getState(), NodeStatus.INACTIVE);
		assertFalse(node.verifyNEM());
	}

	@Test
	public void testNodeURL4() {
		String urlStr = "088.127.0.0.1";
		try {
			new Node(urlStr);
			fail("Creation with <" + urlStr + "> was possible.");
		} catch (IllegalArgumentException ex) {
			// As expected
		}
	}
}
