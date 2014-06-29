package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.math.ColumnVector;
import org.nem.peer.node.*;
import org.nem.peer.trust.score.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class BasicNodeSelectorTest {

	//region recalculations

	@Test
	public void constructorRecalculatesTrustValues() {
		// Act:
		final TestContext context = new TestContext(new ColumnVector(1, 1, 1, 1, 1));

		// Assert:
		Mockito.verify(context.trustProvider, Mockito.times(1)).computeTrust(context.context);
	}

	@Test
	public void selectNodeDoesNotRecalculateTrustValues() {
		// Arrange:
		final TestContext context = new TestContext(new ColumnVector(1, 1, 1, 1, 1));

		// Act:
		for (int i = 0; i < 10; ++i)
			context.selector.selectNode();

		// Assert:
		Mockito.verify(context.trustProvider, Mockito.times(1)).computeTrust(context.context);
	}

	//endregion

	//region selectNode

	@Test
	public void selectNodeReturnsNullWhenAllNodesHaveZeroTrustValues() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(new ColumnVector(0, 0, 0), random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		Assert.assertThat(node, IsNull.nullValue());
	}

	@Test
	public void selectNodeReturnsNonNullNodeWhenAtLeastOneNodeHasNonZeroTrustValue() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(new ColumnVector(0, 1, 0), random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		Assert.assertThat(node, IsEqual.equalTo(context.nodes[1]));
	}

	//endregion

	//region selectNodes

	@Test
	public void selectNodesReturnsEmptyListWhenAllNodesHaveZeroTrustValues() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(new ColumnVector(0, 0, 0, 0), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes(10);

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(0));
	}

	@Test
	public void selectNodesSelectsNodesCorrectlyWhenTrustValuesSumToOne() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(new ColumnVector(0.1, 0.2, 0.3, 0.4), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes(2);

		// Assert:
		Assert.assertThat(
				nodes,
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesSelectsNodesCorrectlyWhenTrustValuesDoNotSumToOne() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(new ColumnVector(20, 40, 60, 80), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes(2);

		// Assert:
		Assert.assertThat(
				nodes,
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesOnlyReturnsUniqueNodesWithNonZeroTrust() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(new ColumnVector(1, 0, 1, 0), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes(10);

		// Assert:
		Assert.assertThat(
				nodes,
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesDoesNotReturnMoreThanMaxNodes() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(new ColumnVector(10, 20, 30, 40), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes(2);

		// Assert:
		Assert.assertThat(
				nodes,
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesReturnsAllNodesIfMaxNodesIsGreaterThanAvailableNodes() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40, 0.10, 0.40);
		final TestContext context = new TestContext(new ColumnVector(10, 20, 30, 40), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes(10);

		// Assert:
		Assert.assertThat(
				nodes,
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2], context.nodes[1], context.nodes[3])));
	}

	private static class TestContext {
		private final TrustContext context = Mockito.mock(TrustContext.class);
		private final TrustProvider trustProvider = Mockito.mock(TrustProvider.class);
		private final Node localNode = Mockito.mock(Node.class);
		private final Node[] nodes;
		private final NodeExperiences nodeExperiences;
		private final NodeSelector selector;

		public TestContext(final ColumnVector trustValues) {
			this(trustValues, new SecureRandom());
		}

		public TestContext(final ColumnVector trustValues, final Random random) {
			Mockito.when(context.getLocalNode()).thenReturn(this.localNode);

			this.nodes = new Node[trustValues.size()];
			for (int i = 0; i < this.nodes.length; ++i)
				this.nodes[i] = new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("127.0.0.1"));

			Mockito.when(this.context.getNodes()).thenReturn(this.nodes);

			this.nodeExperiences = new NodeExperiences();
			Mockito.when(this.context.getNodeExperiences()).thenReturn(this.nodeExperiences);

			Mockito.when(this.trustProvider.computeTrust(this.context)).thenReturn(trustValues);
			this.selector = new BasicNodeSelector(this.trustProvider, this.context, random);
		}
	}
}
