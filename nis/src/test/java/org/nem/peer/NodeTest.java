package org.nem.peer;

import static org.junit.Assert.*;

import org.junit.Test;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.serialization.JsonSerializer;

public class NodeTest {

	@Test
	public void testNodeNULL() {
		try {
			new Node((String) null);
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

	@Test
	public void testNodeSerialize() {
		String urlStr = "127.0.0.1";
		Node node = new Node(urlStr);
		JsonSerializer serializer = new JsonSerializer();
		node.serialize(serializer);
		
		JsonDeserializer deserializer = new JsonDeserializer(serializer.getObject(), null);
		Node node2 = new Node(deserializer);
		assertEquals(node.getAddress(), node2.getAddress());
		assertEquals(node.getPlatform(), node2.getPlatform());
		assertEquals(node.getProtocol(), node2.getProtocol());
		assertEquals(node.getVersion(), node2.getVersion());
	}

	@Test
	public void testNodeJson() {
		String urlStr = "127.0.0.1";
		Node node = new Node(urlStr);
		
		assertNotNull(node.asJsonObject());
	}
}
