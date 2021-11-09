package org.nem.peer.trust;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.node.*;
import org.nem.peer.test.*;

import java.util.*;
import java.util.stream.Collectors;

public class TrustProviderMaskDecoratorTest {

	// region default mask

	@Test
	public void activeNodesAreNotFilteredOut() {
		// Act:
		final ColumnVector resultVector = getFilteredTrustVectorWithStatusAtThirdNode(NodeStatus.ACTIVE);

		// Assert: (4 nodes are active with equally-distributed initial trust)
		MatcherAssert.assertThat(resultVector.getAt(2), IsEqual.equalTo(0.25));
	}

	@Test
	public void inactiveNodesAreFilteredOut() {
		// Act:
		final ColumnVector resultVector = getFilteredTrustVectorWithStatusAtThirdNode(NodeStatus.INACTIVE);

		// Assert:
		MatcherAssert.assertThat(resultVector.getAt(2), IsEqual.equalTo(0.0));
	}

	@Test
	public void failureNodesAreFilteredOut() {
		// Act:
		final ColumnVector resultVector = getFilteredTrustVectorWithStatusAtThirdNode(NodeStatus.FAILURE);

		// Assert:
		MatcherAssert.assertThat(resultVector.getAt(2), IsEqual.equalTo(0.0));
	}

	@Test
	public void localNodeIsFilteredOut() {
		// Act:
		final ColumnVector resultVector = getFilteredTrustVectorWithStatusAtThirdNode(NodeStatus.ACTIVE);

		// Assert:
		MatcherAssert.assertThat(resultVector.getAt(resultVector.size() - 1), IsEqual.equalTo(0.0));
	}

	@Test
	public void zeroVectorIsReturnedWhenAllNodesAreInactive() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector inputVector = new ColumnVector(1, 1, 1, 1, 1);

		final NodeCollection nodeCollection = createNodeCollection(context.getNodes(), NodeStatus.INACTIVE);

		final TrustProvider provider = new TrustProviderMaskDecorator(new MockTrustProvider(context, inputVector), nodeCollection);

		// Act:
		final ColumnVector resultVector = computeTrust(provider);

		// Assert:
		MatcherAssert.assertThat(resultVector, IsEqual.equalTo(new ColumnVector(5)));
	}

	@Test
	public void untrustedActiveNodesAreNotAutoTrustedIfAtLeastOneActiveNodeHasAnyTrust() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector inputVector = new ColumnVector(0, 0.0001, 0, 0, 1);

		final Node[] nodes = context.getNodes();
		final NodeCollection nodeCollection = createNodeCollection(nodes, NodeStatus.ACTIVE);

		final TrustProvider provider = new TrustProviderMaskDecorator(new MockTrustProvider(context, inputVector), nodeCollection);

		// Act:
		final ColumnVector resultVector = computeTrust(provider);

		// Assert:
		MatcherAssert.assertThat(resultVector, IsEqual.equalTo(new ColumnVector(0, 1, 0, 0, 0)));
	}

	@Test
	public void untrustedActiveNodesAreAutoTrustedIfNoActiveNodesHaveAnyTrust() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector inputVector = new ColumnVector(0, 0.0001, 0, 0, 1);

		final Node[] nodes = context.getNodes();
		final NodeCollection nodeCollection = createNodeCollection(nodes, NodeStatus.ACTIVE);
		nodeCollection.update(nodes[1], NodeStatus.INACTIVE);

		final TrustProvider provider = new TrustProviderMaskDecorator(new MockTrustProvider(context, inputVector), nodeCollection);

		// Act:
		final ColumnVector resultVector = computeTrust(provider);

		// Assert:
		MatcherAssert.assertThat(resultVector, IsEqual.equalTo(new ColumnVector(1.0 / 3, 0, 1.0 / 3, 1.0 / 3, 0)));
	}

	@Test
	public void trustIsDistributedProportionallyAmongActiveNodes() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector inputVector = new ColumnVector(3, 1, 5, 2, 1);

		final Node[] nodes = context.getNodes();
		final NodeCollection nodeCollection = createNodeCollection(nodes, NodeStatus.ACTIVE);
		nodeCollection.update(nodes[1], NodeStatus.INACTIVE);

		final TrustProvider provider = new TrustProviderMaskDecorator(new MockTrustProvider(context, inputVector), nodeCollection);

		// Act:
		final ColumnVector resultVector = computeTrust(provider);

		// Assert:
		MatcherAssert.assertThat(resultVector, IsEqual.equalTo(new ColumnVector(0.3, 0, 0.5, 0.2, 0)));
	}

	private static ColumnVector getFilteredTrustVectorWithStatusAtThirdNode(final NodeStatus status) {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector inputVector = new ColumnVector(1, 1, 1, 1, 1);

		final NodeCollection nodeCollection = createNodeCollection(context.getNodes(), NodeStatus.ACTIVE);
		nodeCollection.update(context.getNodes()[2], status);

		final TrustProvider provider = new TrustProviderMaskDecorator(new MockTrustProvider(context, inputVector), nodeCollection);

		// Act:
		return computeTrust(provider);
	}

	private static NodeCollection createNodeCollection(final Node[] nodes, final NodeStatus status) {
		final NodeCollection nodeCollection = new NodeCollection();
		for (final Node node : nodes) {
			nodeCollection.update(node, status);
		}

		return nodeCollection;
	}

	// endregion

	// region predicate context values

	@Test
	public void correctNodesArePassedToCustomPredicate() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();

		final Collection<TrustProviderMaskDecorator.PredicateContext> predicateContexts = getPredicateContextTests(context);

		// Assert:
		MatcherAssert.assertThat(context.getNodes().length, IsEqual.equalTo(5));
		MatcherAssert.assertThat(
				predicateContexts.stream().map(TrustProviderMaskDecorator.PredicateContext::getNode).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(context.getNodes())));
	}

	@Test
	public void correctNodeStatusesArePassedToCustomPredicate() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();

		final Collection<TrustProviderMaskDecorator.PredicateContext> predicateContexts = getPredicateContextTests(context);

		// Assert:
		final Collection<NodeStatus> expectedNodeStatuses = Arrays.asList(NodeStatus.BUSY, NodeStatus.INACTIVE, NodeStatus.FAILURE,
				NodeStatus.ACTIVE, NodeStatus.ACTIVE);
		MatcherAssert.assertThat(context.getNodes().length, IsEqual.equalTo(5));
		MatcherAssert.assertThat(
				predicateContexts.stream().map(TrustProviderMaskDecorator.PredicateContext::getNodeStatus).collect(Collectors.toList()),
				IsEqual.equalTo(expectedNodeStatuses));
	}

	@Test
	public void correctIsLocalNodeStatusesArePassedToCustomPredicate() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();

		final Collection<TrustProviderMaskDecorator.PredicateContext> predicateContexts = getPredicateContextTests(context);

		// Assert:
		MatcherAssert.assertThat(context.getNodes().length, IsEqual.equalTo(5));
		MatcherAssert.assertThat(
				predicateContexts.stream().map(TrustProviderMaskDecorator.PredicateContext::isLocalNode).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(false, false, false, false, true)));
	}

	private static Collection<TrustProviderMaskDecorator.PredicateContext> getPredicateContextTests(final TrustContext context) {
		final ColumnVector inputVector = new ColumnVector(1, 1, 1, 1, 1);

		final NodeCollection nodeCollection = createNodeCollection(context.getNodes(), NodeStatus.ACTIVE);
		nodeCollection.update(context.getNodes()[0], NodeStatus.BUSY);
		nodeCollection.update(context.getNodes()[1], NodeStatus.INACTIVE);
		nodeCollection.update(context.getNodes()[2], NodeStatus.FAILURE);
		nodeCollection.update(context.getNodes()[3], NodeStatus.ACTIVE);

		final List<TrustProviderMaskDecorator.PredicateContext> predicateContexts = new ArrayList<>();
		final TrustProvider provider = new TrustProviderMaskDecorator(new MockTrustProvider(context, inputVector), nodeCollection, pc -> {
			predicateContexts.add(pc);
			return true;
		});

		// Act:
		computeTrust(provider);
		return predicateContexts;
	}

	// endregion

	@Test
	public void innerProviderContextIsUsedAndPropagated() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final TrustContext innerContext = new TestTrustContext().getContext();
		final NodeCollection nodeCollection = createNodeCollection(context.getNodes(), NodeStatus.ACTIVE);

		final ColumnVector vector = new ColumnVector(context.getNodes().length);
		vector.setAll(1);

		final MockTrustProvider innerProvider = new MockTrustProvider(innerContext, vector);
		final TrustProvider provider = new TrustProviderMaskDecorator(innerProvider, nodeCollection);

		// Act:
		final TrustResult result = provider.computeTrust(null);

		// Assert:
		MatcherAssert.assertThat(vector, IsEqual.equalTo(new ColumnVector(0.25, 0.25, 0.25, 0.25, 0)));
		MatcherAssert.assertThat(result.getTrustContext(), IsSame.sameInstance(innerContext));
	}

	private static ColumnVector computeTrust(final TrustProvider provider) {
		// Act: as a decorator, this provider should not used the passed in context for anything so set it to null
		return provider.computeTrust(null).getTrustValues();
	}
}
