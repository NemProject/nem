package org.nem.peer.test;

import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.peer.trust.score.NodeExperience;

import java.util.*;

/**
 * Static class containing helper functions for creating objects used in peer tests.
 */
public class PeerUtils {

	/**
	 * Creates a node array of the specified size.
	 *
	 * @param size The size.
	 * @return The array.
	 */
	public static Node[] createNodeArray(final int size) {
		final Node[] nodes = new Node[size];
		for (int i = 0; i < size; ++i) {
			nodes[i] = NodeUtils.createNodeWithPort(i);
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
			nodes.add(NodeUtils.createNodeWithName(name));
		}

		return nodes;
	}
}
