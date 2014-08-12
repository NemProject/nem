package org.nem.peer.services;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.node.*;

import java.util.*;

public class InactiveNodePrunerTest {

	@Test
	public void pruneInactiveNodesDelegatesToNodeCollection() {
		// Arrange:
		final InactiveNodePruner pruner = new InactiveNodePruner();
		final NodeCollection nodes = Mockito.mock(NodeCollection.class);

		// Act:
		pruner.prune(nodes);

		// Assert:
		Mockito.verify(nodes, Mockito.times(1)).pruneInactiveNodes();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void pruneInactiveNodesReturnsNumberOfNodesPruned() {
		// Arrange:
		final InactiveNodePruner pruner = new InactiveNodePruner();
		final NodeCollection nodes = Mockito.mock(NodeCollection.class);
		Mockito.when(nodes.getInactiveNodes()).thenReturn(createNodeListWithSize(17), createNodeListWithSize(9));

		// Act:
		final int result = pruner.prune(nodes);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(8));
	}

	private static List<Node> createNodeListWithSize(final int size) {
		final List<Node> nodes = new ArrayList<>();
		for (int i = 0; i < size; ++i) {
			nodes.add(Mockito.mock(Node.class));
		}

		return nodes;
	}
}