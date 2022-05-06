package org.nem.peer.test;

import org.hamcrest.MatcherAssert;
import org.nem.core.node.*;
import org.nem.core.test.IsEquivalent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Static class containing asserts that are used to validate NodeCollection objects.
 */
public class NodeCollectionAssert {

	/**
	 * Asserts that nodes have matching active and busy names.
	 *
	 * @param nodes The nodes.
	 * @param expectedActiveNames The expected active names.
	 * @param expectedBusyNames The expected busy names.
	 */
	public static void areNamesEquivalent(final NodeCollection nodes, final String[] expectedActiveNames,
			final String[] expectedBusyNames) {
		// Assert:
		MatcherAssert.assertThat(getNames(nodes.getActiveNodes()), IsEquivalent.equivalentTo(expectedActiveNames));
		MatcherAssert.assertThat(getNames(nodes.getBusyNodes()), IsEquivalent.equivalentTo(expectedBusyNames));
	}

	/**
	 * Asserts that nodes have matching active, busy, inactive, and failure names.
	 *
	 * @param nodes The nodes.
	 * @param expectedActiveNames The expected active names.
	 * @param expectedBusyNames The expected busy names.
	 * @param expectedInactiveNames The expected inactive names.
	 * @param expectedFailureNames The expected failure names.
	 */
	public static void areNamesEquivalent(final NodeCollection nodes, final String[] expectedActiveNames, final String[] expectedBusyNames,
			final String[] expectedInactiveNames, final String[] expectedFailureNames) {
		// Assert:
		MatcherAssert.assertThat(getNames(nodes.getActiveNodes()), IsEquivalent.equivalentTo(expectedActiveNames));
		MatcherAssert.assertThat(getNames(nodes.getBusyNodes()), IsEquivalent.equivalentTo(expectedBusyNames));
		MatcherAssert.assertThat(getNames(nodes.getNodes(NodeStatus.INACTIVE)), IsEquivalent.equivalentTo(expectedInactiveNames));
		MatcherAssert.assertThat(getNames(nodes.getNodes(NodeStatus.FAILURE)), IsEquivalent.equivalentTo(expectedFailureNames));
	}

	private static List<String> getNames(final Collection<Node> nodes) {
		return nodes.stream().map(node -> node.getIdentity().getName()).collect(Collectors.toList());
	}
}
