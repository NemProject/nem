package org.nem.peer.services;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.*;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.core.utils.ExceptionUtils;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.node.NodeCompatibilityChecker;
import org.nem.peer.test.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class NodeRefresherTest {
	private static final int DEFAULT_SLEEP = 300;

	// region getInfo calls

	@Test
	public void refreshCallsGetInfoForAllSpecifiedNodes() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		for (final Node refreshNode : context.refreshNodes) {
			Mockito.verify(context.connector, Mockito.times(1)).getInfo(refreshNode);
		}
	}

	// endregion

	// region getInfo transitions / short-circuiting

	@Test
	public void refreshSuccessMovesNodesToActive() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "b", "c"
		}, new String[]{});
		Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshSuccessCanMoveBusyNodeToActive() {
		// Arrange: set b as "busy"
		final TestContext context = new TestContext();
		context.setBusyGetInfoForNode("b");
		context.refresher.refresh(context.refreshNodes).join();

		// Arrange: reset b to active
		context.setActiveGetInfoForNode("b");

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert: all nodes are active
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "b", "c"
		}, new String[]{});
		Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoTransientFailureMovesNodesToBusy() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setBusyGetInfoForNode("b");

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "c"
		}, new String[]{
				"b"
		});
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoInactiveFailureRemovesNodesFromBothLists() {
		// Assert:
		assertGetInfoFailureRemovesNodesFromBothLists(TestContext::setInactiveGetInfoForNode);
	}

	@Test
	public void refreshGetInfoFatalFailureRemovesNodesFromBothLists() {
		// Assert:
		assertGetInfoFailureRemovesNodesFromBothLists(TestContext::setFatalGetInfoForNode);
	}

	private static void assertGetInfoFailureRemovesNodesFromBothLists(final BiConsumer<TestContext, String> setError) {
		// Arrange:
		final TestContext context = new TestContext();
		setError.accept(context, "b");

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "c"
		}, new String[]{});
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoChangeIdentityRemovesNodesFromBothLists() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node changedNode = NodeUtils.createNodeWithName("p");
		Mockito.when(context.connector.getInfo(context.refreshNodes.get(1))).thenReturn(CompletableFuture.completedFuture(changedNode));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "c"
		}, new String[]{});
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(context.refreshNodes.get(1));
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(changedNode);
	}

	@Test
	public void refreshGetInfoIncompatibleNodeRemovesNodesFromBothLists() {
		// Arrange:
		final NodeCompatibilityChecker versionCheck = Mockito.mock(NodeCompatibilityChecker.class);
		final TestContext context = new TestContext(versionCheck);
		Mockito.when(versionCheck.check(Mockito.any(), Mockito.any())).thenReturn(true);

		final Node incompatibleNode = context.refreshNodes.get(1);
		incompatibleNode.setMetaData(new NodeMetaData("p", "a", new NodeVersion(1, 0, 0), 7, 4));
		final NodeMetaData incompatibleNodeMetaData = incompatibleNode.getMetaData();
		Mockito.when(versionCheck.check(context.localNode.getMetaData(), incompatibleNodeMetaData)).thenReturn(false);

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "c"
		}, new String[]{});
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoChangeAddressUpdatesNodeEndpoint() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getInfo(context.refreshNodes.get(1)))
				.thenReturn(CompletableFuture.completedFuture(NodeUtils.createNodeWithHost("10.0.0.125", "b")));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();
		final Node updatedNode = context.nodes.findNodeByIdentity(new WeakNodeIdentity("b"));

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "b", "c"
		}, new String[]{});
		MatcherAssert.assertThat(updatedNode.getEndpoint(), IsEqual.equalTo(NodeEndpoint.fromHost("10.0.0.125")));
		Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoChangeMetaDataUpdatesNodeMetaData() {
		// Arrange:
		final NodeMetaData expectedMetaData = new NodeMetaData("c-plat", "c-app");
		final TestContext context = new TestContext();
		final Node originalNode = context.refreshNodes.get(1);
		Mockito.when(context.connector.getInfo(context.refreshNodes.get(1))).thenReturn(
				CompletableFuture.completedFuture(new Node(originalNode.getIdentity(), originalNode.getEndpoint(), expectedMetaData)));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();
		final Node updatedNode = context.nodes.findNodeByIdentity(new WeakNodeIdentity("b"));
		final NodeMetaData metaData = updatedNode.getMetaData();

		// Assert:
		MatcherAssert.assertThat(metaData, IsEqual.equalTo(expectedMetaData));
		Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoChangeNameUpdatesName() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node originalNode = context.refreshNodes.get(1);
		Mockito.when(context.connector.getInfo(originalNode)).thenReturn(CompletableFuture.completedFuture(
				new Node(new WeakNodeIdentity("b", "b-new-name"), originalNode.getEndpoint(), originalNode.getMetaData())));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();
		final Node updatedNode = context.nodes.findNodeByIdentity(new WeakNodeIdentity("b"));
		final NodeIdentity identity = updatedNode.getIdentity();

		// Assert:
		MatcherAssert.assertThat(identity.getName(), IsEqual.equalTo("b-new-name"));
		Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(context.refreshNodes.get(1));
	}

	// endregion

	// region getKnownPeers calls

	@Test
	public void refreshCallsGetKnownPeersForActiveNodes() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		for (final Node refreshNode : context.refreshNodes) {
			Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(refreshNode);
		}
	}

	// endregion

	// region getKnownPeers transitions / short-circuiting

	@Test
	public void refreshGetKnownPeersInfoTransientFailureMovesNodesToBusy() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getKnownPeers(context.refreshNodes.get(1))).thenReturn(CompletableFuture.supplyAsync(() -> {
			throw new BusyPeerException("busy");
		}));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "c"
		}, new String[]{
				"b"
		});
	}

	@Test
	public void refreshGetKnownPeersInactiveFailureRemovesNodesFromBothLists() {
		// Assert:
		assertGetKnownPeersFailureRemovesNodesFromBothLists(new InactivePeerException("inactive"));
	}

	@Test
	public void refreshGetKnownPeersFatalFailureRemovesNodesFromBothLists() {
		// Assert:
		assertGetKnownPeersFailureRemovesNodesFromBothLists(new FatalPeerException("fatal"));
	}

	private static void assertGetKnownPeersFailureRemovesNodesFromBothLists(final RuntimeException ex) {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getKnownPeers(context.refreshNodes.get(1))).thenReturn(CompletableFuture.supplyAsync(() -> {
			throw ex;
		}));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "c"
		}, new String[]{});
	}

	// endregion

	// region precedence / potential attacks

	@Test
	public void evilNodeCannotChangeNameOfKnownPeer() {
		// Arrange:
		// - in the known peers list, indicate the name of 'b' is really 'bad bob'
		final TestContext context = new TestContext();
		final List<Node> knownPeers = PeerUtils.createNodesWithNames("b");
		knownPeers.get(0).getIdentity().setName("bad bob");
		context.setKnownPeers(knownPeers);

		// Act:
		context.refresher.refresh(context.refreshNodes).join();
		final Node updatedNode = context.nodes.findNodeByIdentity(new WeakNodeIdentity("b"));
		final NodeIdentity identity = updatedNode.getIdentity();

		// Assert:
		// - since 'b' was communicated with directly, the evil peer cannot update the name
		MatcherAssert.assertThat(identity.getName(), IsEqual.equalTo("b"));
	}

	@Test
	public void evilNodeCanProvideNameOfUnknownPeer() {
		// Arrange:
		// - in the known peers list, indicate the name of 'b' is really 'bad bob'
		final TestContext context = new TestContext();
		final List<Node> knownPeers = PeerUtils.createNodesWithNames("z");
		knownPeers.get(0).getIdentity().setName("bad bob");
		context.setKnownPeers(knownPeers);

		// Act:
		context.refresher.refresh(context.refreshNodes).join();
		final Node updatedNode = context.nodes.findNodeByIdentity(new WeakNodeIdentity("z"));
		final NodeIdentity identity = updatedNode.getIdentity();

		// Assert:
		// - since 'b' was not communicated with directly, the evil peer can provide the name
		MatcherAssert.assertThat(identity.getName(), IsEqual.equalTo("bad bob"));
	}

	@Test
	public void refreshDoesNotUpdateIndirectNodesWithNonActiveStatusWhenNodesAreActive() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setBusyGetInfoForNode("a", DEFAULT_SLEEP);
		context.setBusyGetInfoForNode("b", DEFAULT_SLEEP);
		context.setFatalGetInfoForNode("d");
		context.setBusyGetInfoForNode("f");
		context.setRuntimeExceptionGetInfoForNode("g");
		context.nodes.update(NodeUtils.createNodeWithName("d"), NodeStatus.ACTIVE);
		context.nodes.update(NodeUtils.createNodeWithName("f"), NodeStatus.ACTIVE);
		context.nodes.update(NodeUtils.createNodeWithName("g"), NodeStatus.ACTIVE);

		// Arrange: set up a node peers list that indicates peers b, d-g are active
		// but the local node can only communicate with e, other nodes throw exceptions
		// since nodes d, f, g are already active, they stay active
		context.setKnownPeers(PeerUtils.createNodesWithNames("b", "d", "e", "f", "g"));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		// - all good peers (c, d, f, g) that were directly communicated with are active even though
		// some are reported as being in a bad state by other peers (d, f, g)
		// - indirect peers (e) that were communicated with successfully are active
		// - busy peers communicated with directly are busy (a, b)
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"c", "d", "e", "f", "g"
		}, new String[]{
				"a", "b"
		});
		MatcherAssert.assertThat(context.nodes.size(), IsEqual.equalTo(7));
	}

	@Test
	public void refreshDoesNotUpdateIndirectNodesWithNonActiveStatusWhenNodesAreNotActive() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setBusyGetInfoForNode("a", DEFAULT_SLEEP);
		context.setBusyGetInfoForNode("b", DEFAULT_SLEEP);
		context.setFatalGetInfoForNode("d");
		context.setBusyGetInfoForNode("f");
		context.setRuntimeExceptionGetInfoForNode("g");
		context.nodes.update(NodeUtils.createNodeWithName("d"), NodeStatus.BUSY);
		context.nodes.update(NodeUtils.createNodeWithName("f"), NodeStatus.FAILURE);
		context.nodes.update(NodeUtils.createNodeWithName("g"), NodeStatus.INACTIVE);

		// Arrange: set up a node peers list that indicates peers b, d-g are active
		// but the local node can only communicate with e, other nodes throw exceptions
		// since nodes d, f, g are already in the node collection, they keep their original statuses
		context.setKnownPeers(PeerUtils.createNodesWithNames("b", "d", "e", "f", "g"));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		// - all peers (a, b, c, d, f, g) that were directly communicated preserve their original statuses
		// - indirect peers (e) that were communicated with successfully are active
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"c", "e"
		}, new String[]{
				"a", "b", "d"
		}, new String[]{
				"g"
		}, new String[]{
				"f"
		});
		MatcherAssert.assertThat(context.nodes.size(), IsEqual.equalTo(7));
	}

	@Test
	public void refreshGivesPrecedenceToFirstHandExperience() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setBusyGetInfoForNode("b", DEFAULT_SLEEP);
		context.setFatalGetInfoForNode("d");
		context.setBusyGetInfoForNode("f");
		context.setFatalGetInfoForNode("g");

		// Arrange: set up a node peers list that indicates peers b, d-g are active
		// but the local node can only communicate with e
		context.setKnownPeers(PeerUtils.createNodesWithNames("b", "d", "e", "f", "g"));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		// - all good peers (a, c) that were directly communicated with are active
		// - all good peers (e) that were indirectly communicated with are active
		// - all busy peers (b) that were directly communicated with are busy
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "c", "e"
		}, new String[]{
				"b"
		});
		MatcherAssert.assertThat(context.nodes.size(), IsEqual.equalTo(4));
	}

	@Test
	public void refreshPreventsEvilNodeFromGettingGoodNodesDropped() {
		// this is similar to refreshGivesPrecedenceToFirstHandExperience
		// but is important to show the following attack is prevented:
		// evil node propagating mismatched identities for good nodes does not remove the good nodes

		// Arrange: set up 4 active nodes (3 pre-trusted)
		final TestContext context = new TestContext();
		context.refreshNodes.add(NodeUtils.createNodeWithName("d"));
		context.setBusyGetInfoForNode("c", DEFAULT_SLEEP);

		// Arrange: when the mock connector sees nodes w-x, it will trigger an identity change to a good node
		final List<Node> evilNodes = Arrays.asList(NodeUtils.createNodeWithHost("10.0.0.100", "w"),
				NodeUtils.createNodeWithHost("10.0.0.101", "x"), NodeUtils.createNodeWithHost("10.0.0.102", "y"),
				NodeUtils.createNodeWithHost("10.0.0.103", "z"));
		context.setKnownPeers(evilNodes);
		for (final Node evilNode : evilNodes) {
			final StringBuilder newNameBuilder = new StringBuilder();
			newNameBuilder.appendCodePoint(evilNode.getIdentity().getName().codePointAt(0) - 'w' + 'b');
			Mockito.when(context.connector.getInfo(evilNode))
					.thenReturn(CompletableFuture.completedFuture(NodeUtils.createNodeWithName(newNameBuilder.toString())));
		}

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		// - all good peers (a, b, d) that were directly communicated with are active
		// - the delayed inactive node (c) is inactive
		// - the impersonating bad nodes are not added
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "b", "d"
		}, new String[]{
				"c"
		});
		MatcherAssert.assertThat(context.nodes.size(), IsEqual.equalTo(4));

		// Assert: the endpoints of good nodes were not changed
		for (final Node node : context.nodes.getAllNodes()) {
			MatcherAssert.assertThat(node.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.1"));
		}
	}

	@Test
	public void refreshResultForDirectNodeIgnoresChildNodeGetInfoResults() {
		// Arrange: set up 4 active nodes (3 pre-trusted)
		final TestContext context = new TestContext();
		context.refreshNodes.add(NodeUtils.createNodeWithName("d"));
		context.setBusyGetInfoForNode("c", DEFAULT_SLEEP);
		context.setFatalGetInfoForNode("y");
		context.setBusyGetInfoForNode("z");

		// Arrange: set up a node peers list that contains an unseen inactive and failure node
		context.setKnownPeers(PeerUtils.createNodesWithNames("y", "z"));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		// - all peers (a, b, d) that were directly communicated with successfully are active
		// - the peer that was directly communicated with unsuccessfully (c) is inactive
		// - the unseen inactive peer (z) is not added because the information cannot be trusted
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "b", "d"
		}, new String[]{
				"c"
		});
		MatcherAssert.assertThat(context.nodes.size(), IsEqual.equalTo(4));
	}

	@Test
	public void refreshCallsGetInfoOnceForEachUniqueEndpoint() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setBusyGetInfoForNode("b", DEFAULT_SLEEP);
		context.setFatalGetInfoForNode("d");
		context.setBusyGetInfoForNode("f");

		// Arrange: set up a node peers list that indicates peer b, d-f are active
		// but the local node can only communicate with e
		context.setKnownPeers(PeerUtils.createNodesWithNames("b", "d", "e", "f"));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		Mockito.verify(context.connector, Mockito.times(2)).getKnownPeers(Mockito.any()); // a c
		Mockito.verify(context.connector, Mockito.times(6)).getInfo(Mockito.any()); // a b c d e f
	}

	@Test
	public void refreshGetInfoIsBypassedForBlacklistedNodes() {
		// Arrange: blacklist a and c
		final TestContext context = new TestContext();
		context.nodes.update(NodeUtils.createNodeWithName("a"), NodeStatus.FAILURE);
		context.nodes.update(NodeUtils.createNodeWithName("c"), NodeStatus.INACTIVE);
		context.nodes.update(NodeUtils.createNodeWithName("d"), NodeStatus.FAILURE);
		context.nodes.update(NodeUtils.createNodeWithName("f"), NodeStatus.INACTIVE);

		// Arrange: set up a node peers list that indicates peer d-f are active
		// but the local node can only communicate with e
		context.setKnownPeers(PeerUtils.createNodesWithNames("d", "e", "f"));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"b", "e"
		}, new String[]{});
		Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(Mockito.any()); // b
		Mockito.verify(context.connector, Mockito.times(2)).getInfo(Mockito.any()); // b, e
	}

	// endregion

	// region basic merging

	@Test
	public void refreshOnlyMergesInRelayedActivePeers() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Node> knownPeers = PeerUtils.createNodesWithNames("y", "z");
		Mockito.when(context.connector.getKnownPeers(Mockito.any()))
				.thenReturn(CompletableFuture.completedFuture(new SerializableList<>(knownPeers)));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "b", "c", "y", "z"
		}, new String[]{});
	}

	@Test
	public void refreshDoesNotMergeInLocalNode() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getKnownPeers(Mockito.any()))
				.thenReturn(CompletableFuture.completedFuture(new SerializableList<>(Collections.singletonList(context.localNode))));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[]{
				"a", "b", "c"
		}, new String[]{});
	}

	// endregion

	// region async

	@Test
	public void refreshIsAsync() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setBusyGetInfoForNode("b", DEFAULT_SLEEP);

		// Act:
		final CompletableFuture<?> future = context.refresher.refresh(context.refreshNodes);

		// Assert:
		MatcherAssert.assertThat(future.isDone(), IsEqual.equalTo(false));
	}

	// endregion

	private static PeerConnector mockPeerConnector() {
		final PeerConnector connector = Mockito.mock(PeerConnector.class);
		Mockito.when(connector.getInfo(Mockito.any())).thenAnswer(i -> CompletableFuture.completedFuture((Node) i.getArguments()[0]));
		Mockito.when(connector.getKnownPeers(Mockito.any())).thenAnswer(i -> CompletableFuture.completedFuture(new SerializableList<>(10)));
		return connector;
	}

	private static class TestContext {
		private final List<Node> refreshNodes = PeerUtils.createNodesWithNames("a", "b", "c");
		private final Node localNode = NodeUtils.createNodeWithName("l");
		private final NodeCollection nodes = new NodeCollection();
		private final PeerConnector connector = mockPeerConnector();
		private final NodeRefresher refresher;

		public TestContext() {
			this((l, r) -> true);
		}

		public TestContext(final NodeCompatibilityChecker versionCheck) {
			this.refresher = new NodeRefresher(this.localNode, this.nodes, this.connector, versionCheck);
		}

		public void setActiveGetInfoForNode(final String name) {
			final Node node = NodeUtils.createNodeWithName(name);
			Mockito.when(this.connector.getInfo(node)).thenAnswer(i -> CompletableFuture.completedFuture((Node) i.getArguments()[0]));
		}

		public void setBusyGetInfoForNode(final String name) {
			this.setBusyGetInfoForNode(name, 0);
		}

		public void setBusyGetInfoForNode(final String name, final int sleep) {
			final Node node = NodeUtils.createNodeWithName(name);
			Mockito.when(this.connector.getInfo(node)).thenReturn(CompletableFuture.supplyAsync(() -> {
				ExceptionUtils.propagateVoid(() -> Thread.sleep(sleep));
				throw new BusyPeerException("busy");
			}));
		}

		public void setInactiveGetInfoForNode(final String name) {
			final Node node = NodeUtils.createNodeWithName(name);
			Mockito.when(this.connector.getInfo(node)).thenReturn(CompletableFuture.supplyAsync(() -> {
				throw new InactivePeerException("inactive");
			}));
		}

		public void setFatalGetInfoForNode(final String name) {
			final Node node = NodeUtils.createNodeWithName(name);
			Mockito.when(this.connector.getInfo(node)).thenReturn(CompletableFuture.supplyAsync(() -> {
				throw new FatalPeerException("fatal");
			}));
		}

		public void setRuntimeExceptionGetInfoForNode(final String name) {
			final Node node = NodeUtils.createNodeWithName(name);
			Mockito.when(this.connector.getInfo(node)).thenReturn(CompletableFuture.supplyAsync(() -> {
				throw new RuntimeException("runtime exception");
			}));
		}

		public void setKnownPeers(final List<Node> nodes) {
			Mockito.when(this.connector.getKnownPeers(Mockito.any()))
					.thenAnswer(i -> CompletableFuture.completedFuture(new SerializableList<>(nodes)));
		}
	}
}
