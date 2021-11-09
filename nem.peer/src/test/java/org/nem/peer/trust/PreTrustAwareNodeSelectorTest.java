package org.nem.peer.trust;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.node.*;
import org.nem.core.test.*;
import org.nem.peer.test.PeerUtils;

import java.security.SecureRandom;
import java.util.*;

public class PreTrustAwareNodeSelectorTest {

	// region selectNode

	@Test
	public void selectNodeDelegatesToWrappedSelector() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node node = NodeUtils.createNodeWithName("a");
		Mockito.when(context.innerSelector.selectNode()).thenReturn(node);

		// Act:
		final Node selectedNode = context.selector.selectNode();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNode();
		MatcherAssert.assertThat(selectedNode, IsSame.sameInstance(node));
	}

	@Test
	public void selectNodeReturnsRandomOfflinePreTrustedNodeWhenAllPreTrustedNodesAreOffline() {
		// Arrange:
		final TestContext context = new TestContext(PeerUtils.createNodesWithNames("p", "q", "r", "s"), createMockRandom(0.6));

		// Act:
		final Node selectedNode = context.selector.selectNode();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNode();
		MatcherAssert.assertThat(selectedNode, IsEqual.equalTo(NodeUtils.createNodeWithName("r")));
	}

	@Test
	public void selectNodeReturnsRandomOnlinePreTrustedNodeWhenSomePreTrustedNodesAreOnlineAndLocalNodeIsPreTrusted() {
		// Arrange:
		final TestContext context = new TestContext(PeerUtils.createNodesWithNames("p-a", "q-a", "r-f", "s-a", "t-i", "l"),
				createMockRandom(0.5));
		context.setTestNodeStatuses();

		// Act:
		final Node selectedNode = context.selector.selectNode();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNode();
		MatcherAssert.assertThat(selectedNode, IsEqual.equalTo(NodeUtils.createNodeWithName("q-a")));
	}

	@Test
	public void selectNodeReturnsRandomOnlinePreTrustedNodeWhenSomePreTrustedNodesAreOnlineAndLocalNodeIsNotPreTrusted() {
		// Arrange:
		final TestContext context = new TestContext(PeerUtils.createNodesWithNames("p-a", "q-a", "r-f", "s-a", "t-i"),
				createMockRandom(0.5, 0.1));
		context.setTestNodeStatuses();

		// Act:
		final Node selectedNode = context.selector.selectNode();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNode();
		MatcherAssert.assertThat(selectedNode, IsEqual.equalTo(NodeUtils.createNodeWithName("q-a")));
	}

	// endregion

	// region selectNodes

	@Test
	public void selectNodesAddsAllPreTrustedNodesWhenAllAreOffline() {
		// Arrange:
		final TestContext context = new TestContext(PeerUtils.createNodesWithNames("p", "q", "r"));
		final List<Node> nodes = PeerUtils.createNodesWithNames("a", "p", "c");
		Mockito.when(context.innerSelector.selectNodes()).thenReturn(nodes);

		// Act:
		final List<Node> selectedNodes = context.selector.selectNodes();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNodes();
		MatcherAssert.assertThat(selectedNodes, IsEquivalent.equivalentTo(PeerUtils.createNodesWithNames("p", "q", "r", "a", "c")));
	}

	@Test
	public void selectNodesForLocalPreTrustedNodeAddsAllOtherOnlinePreTrustedNodes() {
		// Arrange:
		final TestContext context = new TestContext(PeerUtils.createNodesWithNames("p-a", "q-a", "r-f", "s-a", "t-i", "l"));
		context.setTestNodeStatuses();
		Mockito.when(context.innerSelector.selectNodes()).thenReturn(PeerUtils.createNodesWithNames("a", "p-a", "c"));

		// Act:
		final List<Node> selectedNodes = context.selector.selectNodes();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNodes();
		MatcherAssert.assertThat(selectedNodes, IsEquivalent.equivalentTo(PeerUtils.createNodesWithNames("a", "c", "p-a", "q-a", "s-a")));
	}

	@Test
	public void selectNodesForLocalNonPreTrustedNodeAddsRandomPreTrustedNodes() {
		// Arrange:
		final TestContext context = createContextForLocalNonPreTrustedNodeSelectNodes();
		Mockito.when(context.innerSelector.selectNodes()).thenReturn(PeerUtils.createNodesWithNames("a", "p-a", "c"));

		// Act:
		final List<Node> selectedNodes = context.selector.selectNodes();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNodes();
		MatcherAssert.assertThat(selectedNodes, IsEquivalent.equivalentTo(PeerUtils.createNodesWithNames("a", "c", "p-a", "q-a")));
	}

	@Test
	public void selectNodesForLocalNonPreTrustedNodeDoesNotAddRandomPreTrustedNodeIfNodeIsAlreadySelected() {
		// Arrange:
		final TestContext context = createContextForLocalNonPreTrustedNodeSelectNodes();
		Mockito.when(context.innerSelector.selectNodes()).thenReturn(PeerUtils.createNodesWithNames("a", "q-a", "c"));

		// Act:
		final List<Node> selectedNodes = context.selector.selectNodes();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNodes();
		MatcherAssert.assertThat(selectedNodes, IsEquivalent.equivalentTo(PeerUtils.createNodesWithNames("a", "c", "q-a")));
	}

	// endregion

	private static Random createMockRandom(final Double value, final Double... values) {
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(value, values);
		return random;
	}

	private static TestContext createContextForLocalNonPreTrustedNodeSelectNodes() {
		final TestContext context = new TestContext(PeerUtils.createNodesWithNames("p-a", "q-a", "r-f", "s-a", "t-i"),
				createMockRandom(0.5));
		context.setTestNodeStatuses();
		return context;
	}

	private static class TestContext {
		private final NodeSelector innerSelector = Mockito.mock(NodeSelector.class);
		private final TrustContext context = Mockito.mock(TrustContext.class);

		private final Node localNode = NodeUtils.createNodeWithName("l");
		private final NodeCollection nodes = new NodeCollection();
		private final NodeSelector selector;

		public TestContext() {
			this(PeerUtils.createNodesWithNames("p", "q", "r"));
		}

		public TestContext(final List<Node> preTrustedNodes) {
			this(preTrustedNodes, new SecureRandom());
		}

		public TestContext(final List<Node> preTrustedNodes, final Random random) {
			Mockito.when(this.context.getLocalNode()).thenReturn(this.localNode);
			Mockito.when(this.context.getPreTrustedNodes()).thenReturn(new PreTrustedNodes(new LinkedHashSet<>(preTrustedNodes)));

			this.selector = new PreTrustAwareNodeSelector(this.innerSelector, this.nodes, this.context, random);
		}

		public void setNodeStatus(final String name, final NodeStatus status) {
			this.nodes.update(NodeUtils.createNodeWithName(name), status);
		}

		public void setTestNodeStatuses() {
			this.setNodeStatus("p-a", NodeStatus.ACTIVE);
			this.setNodeStatus("q-a", NodeStatus.ACTIVE);
			this.setNodeStatus("r-f", NodeStatus.FAILURE);
			this.setNodeStatus("s-a", NodeStatus.ACTIVE);
			this.setNodeStatus("t-i", NodeStatus.INACTIVE);
			this.setNodeStatus("l", NodeStatus.ACTIVE);
		}
	}
}
