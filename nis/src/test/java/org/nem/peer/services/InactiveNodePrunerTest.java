package org.nem.peer.services;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.node.*;

public class InactiveNodePrunerTest {

	@Test
	public void pruneInactiveNodesDelegatesToNodeCollection() {
		// Arrange:
		final InactiveNodePruner pruner = new InactiveNodePruner();
		final NodeCollection nodes = Mockito.mock(NodeCollection.class);

		// Act:
		pruner.prune(nodes);

		// Assert:
		Mockito.verify(nodes, Mockito.times(1)).prune();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void pruneInactiveNodesReturnsNumberOfNodesPruned() {
		// Arrange:
		final InactiveNodePruner pruner = new InactiveNodePruner();
		final NodeCollection nodes = Mockito.mock(NodeCollection.class);
		Mockito.when(nodes.size()).thenReturn(17, 9);

		// Act:
		final int result = pruner.prune(nodes);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(8));
	}
}