package org.nem.core.test;

import org.nem.core.node.*;

/**
 * Static class containing helper functions for creating node objects.
 */
public class NodeUtils {

	/**
	 * Creates a node with the specified name.
	 *
	 * @param name The name.
	 * @return The new node.
	 */
	public static Node createNodeWithName(final String name) {
		return createNodeWithHost("127.0.0.1", name);
	}

	/**
	 * Creates a node with the specified port.
	 * Note: this is around for legacy tests that are using port as a hallmark.
	 *
	 * @param port The port.
	 * @return The new node.
	 */
	public static Node createNodeWithPort(final int port) {
		return new Node(
				new WeakNodeIdentity(String.format("%d", port)),
				new NodeEndpoint("http", "127.0.0.1", port));
	}

	/**
	 * Creates a node with the specified host name.
	 *
	 * @param host The host.
	 * @return The new node.
	 */
	public static Node createNodeWithHost(final String host) {
		return createNodeWithHost(host, host);
	}

	/**
	 * Creates a node with the specified host name.
	 *
	 * @param host The host.
	 * @param name The name.
	 * @return The new node.
	 */
	public static Node createNodeWithHost(final String host, final String name) {
		return new Node(new WeakNodeIdentity(name), NodeEndpoint.fromHost(host));
	}
}
