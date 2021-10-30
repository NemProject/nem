package org.nem.peer.trust;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.math.ColumnVector;
import org.nem.core.node.*;
import org.nem.peer.trust.score.NodeExperiences;

import java.util.*;

/**
 * Interface test for NodeSelector
 */
public abstract class NodeSelectorTest {

	/**
	 * Creates the node selector to test.
	 */
	protected abstract NodeSelector createSelector(final int maxNodes, final ColumnVector trustVector, final TrustContext context,
			final Random random);

	// region selectNode

	@Test
	public void selectNodeReturnsNullWhenAllNodesHaveZeroTrustValues() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(10, new ColumnVector(0, 0, 0), random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		MatcherAssert.assertThat(node, IsNull.nullValue());
	}

	@Test
	public void selectNodeReturnsNonNullNodeWhenAtLeastOneNodeHasNonZeroTrustValue() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(10, new ColumnVector(0, 1, 0), random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		MatcherAssert.assertThat(node, IsEqual.equalTo(context.nodes[1]));
	}

	// endregion

	// region selectNodes

	@Test
	public void selectNodesReturnsEmptyListWhenAllNodesHaveZeroTrustValues() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(10, new ColumnVector(0, 0, 0, 0), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes.size(), IsEqual.equalTo(0));
	}

	@Test
	public void selectNodesSelectsNodesCorrectlyWhenTrustValuesSumToOne() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(2, new ColumnVector(0.1, 0.2, 0.3, 0.4), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes, IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesSelectsNodesCorrectlyWhenTrustValuesDoNotSumToOne() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(2, new ColumnVector(20, 40, 60, 80), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes, IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesOnlyReturnsUniqueNodesWithNonZeroTrust() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(10, new ColumnVector(1, 0, 1, 0), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes, IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesDoesNotReturnMoreThanMaxNodes() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(2, new ColumnVector(10, 20, 30, 40), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes, IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesReturnsAllNodesIfMaxNodesIsGreaterThanAvailableNodes() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40, 0.10, 0.40);
		final TestContext context = new TestContext(10, new ColumnVector(10, 20, 30, 40), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes, IsEqual.equalTo(Arrays.asList(context.nodes)));

		// - assert that it took the shortcut
		Mockito.verify(random, Mockito.never()).nextDouble();
	}

	private class TestContext {
		private final TrustContext context = Mockito.mock(TrustContext.class);
		private final Node localNode = Mockito.mock(Node.class);
		private final Node[] nodes;
		private final NodeExperiences nodeExperiences;
		private final NodeSelector selector;

		public TestContext(final int maxNodes, final ColumnVector trustValues, final Random random) {
			Mockito.when(this.context.getLocalNode()).thenReturn(this.localNode);

			this.nodes = new Node[trustValues.size()];
			for (int i = 0; i < this.nodes.length; ++i) {
				this.nodes[i] = new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("127.0.0.1"));
			}

			Mockito.when(this.context.getNodes()).thenReturn(this.nodes);

			this.nodeExperiences = new NodeExperiences();
			Mockito.when(this.context.getNodeExperiences()).thenReturn(this.nodeExperiences);

			this.selector = NodeSelectorTest.this.createSelector(maxNodes, trustValues, this.context, random);
		}
	}
}
