package org.nem.peer.test;

import org.nem.peer.node.*;
import org.nem.peer.trust.score.NodeExperience;

public class Utils {
	/**
	 * Creates a node with the specified port number.
	 *
	 * @param port The port number.
	 *
	 * @return The new node.
	 */
	public static Node createNodeWithPort(final int port) {
		return new Node(new NodeEndpoint("http", "localhost", port), "P", "A", "V");
	}

	/**
	 * Creates a node array of the specified size.
	 *
	 * @param size The size.
	 *
	 * @return The array.
	 */
	public static Node[] createNodeArray(int size) {
		final Node[] nodes = new Node[size];
		for (int i = 0; i < size; ++i)
			nodes[i] = Utils.createNodeWithPort(80 + i);

		return nodes;
	}

	/**
	 * Creates a new node experience with the specified number of calls.
	 *
	 * @param numSuccessfulCalls The number of successful calls.
	 *
	 * @return The node experience.
	 */
	public static NodeExperience createNodeExperience(final long numSuccessfulCalls) {
		final NodeExperience experience = new NodeExperience();
		experience.successfulCalls().set(numSuccessfulCalls);
		return experience;
	}
}
