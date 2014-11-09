package org.nem.peer.test;

import org.junit.Assert;
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
	public static void areNamesEquivalent(
			final NodeCollection nodes,
			final String[] expectedActiveNames,
			final String[] expectedBusyNames) {
		// Assert:
		Assert.assertThat(getNames(nodes.getActiveNodes()), IsEquivalent.equivalentTo(expectedActiveNames));
		Assert.assertThat(getNames(nodes.getBusyNodes()), IsEquivalent.equivalentTo(expectedBusyNames));
	}

	private static List<String> getNames(final Collection<Node> nodes) {
		return nodes.stream()
				.map(node -> node.getIdentity().getName())
				.collect(Collectors.toList());
	}
}
