package org.nem.peer.test;

import org.nem.core.node.*;
import org.nem.peer.trust.score.NodeExperience;

import java.util.*;

public class PeerUtils {

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

	/**
	 * Creates a node array of the specified size.
	 *
	 * @param size The size.
	 * @return The array.
	 */
	public static Node[] createNodeArray(final int size) {
		final Node[] nodes = new Node[size];
		for (int i = 0; i < size; ++i) {
			nodes[i] = createNodeWithPort(i);
		}

		return nodes;
	}

	/**
	 * Creates a new node experience with the specified number of calls.
	 *
	 * @param numSuccessfulCalls The number of successful calls.
	 * @return The node experience.
	 */
	public static NodeExperience createNodeExperience(final long numSuccessfulCalls) {
		final NodeExperience experience = new NodeExperience();
		experience.successfulCalls().set(numSuccessfulCalls);
		return experience;
	}

	/**
	 * Creates a list of nodes with the specified names.
	 *
	 * @param names The desired names.
	 * @return The nodes.
	 */
	public static List<Node> createNodesWithNames(final String... names) {
		final List<Node> nodes = new ArrayList<>();
		for (final String name : names) {
			nodes.add(PeerUtils.createNodeWithName(name));
		}

		return nodes;
	}
}
