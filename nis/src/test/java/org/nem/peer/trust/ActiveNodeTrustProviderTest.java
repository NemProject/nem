package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.peer.node.*;
import org.nem.peer.test.MockTrustProvider;
import org.nem.peer.test.TestTrustContext;

public class ActiveNodeTrustProviderTest {

	@Test
	public void activeNodesAreNotFilteredOut() {
		// Act:
		final ColumnVector vector = getFilteredTrustVector(NodeStatus.ACTIVE);

		// Assert:
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(1.0));
	}

	@Test
	public void inactiveNodesAreFilteredOut() {
		// Act:
		final ColumnVector vector = getFilteredTrustVector(NodeStatus.INACTIVE);

		// Assert:
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.0));
	}

	@Test
	public void failureNodesAreFilteredOut() {
		// Act:
		final ColumnVector vector = getFilteredTrustVector(NodeStatus.FAILURE);

		// Assert:
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.0));
	}

	@Test
	public void localNodeIsFilteredOut() {
		// Act:
		final ColumnVector vector = getFilteredTrustVector(NodeStatus.ACTIVE);

		// Assert:
		Assert.assertThat(vector.getAt(vector.getSize() - 1), IsEqual.equalTo(0.0));
	}

	private static ColumnVector getFilteredTrustVector(final NodeStatus status) {
		// Arrange:
		final TestTrustContext testContext = new TestTrustContext();
		final TrustContext context = testContext.getContext();
		final ColumnVector vector = new ColumnVector(context.getNodes().length);
		vector.setAll(1);

		final Node[] nodes = context.getNodes();
		final NodeCollection nodeCollection = new NodeCollection();
		for (final Node node : nodes)
			nodeCollection.update(node, NodeStatus.ACTIVE);

		nodeCollection.update(nodes[2], status);

		final TrustProvider provider = new ActiveNodeTrustProvider(new MockTrustProvider(vector), nodeCollection);

		// Act:
		return provider.computeTrust(context);
	}
}
