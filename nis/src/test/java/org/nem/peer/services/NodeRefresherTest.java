package org.nem.peer.services;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.*;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.utils.ExceptionUtils;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.node.NodeVersionCheck;
import org.nem.peer.test.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class NodeRefresherTest {
	private static final int DEFAULT_SLEEP = 300;

	//region getInfo calls

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

	//endregion

	//region getInfo transitions / short-circuiting

	@Test
	public void refreshSuccessMovesNodesToActive() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "b", "c" }, new String[] { });
		Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoTransientFailureMovesNodesToInactive() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setBusyGetInfoForNode("b");

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "c" }, new String[] { "b" });
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoInactiveFailureRemovesNodesFromBothLists() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setInactiveGetInfoForNode("b");

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "c" }, new String[] { });
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoFatalFailureRemovesNodesFromBothLists() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setFatalGetInfoForNode("b");

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "c" }, new String[] { });
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoChangeIdentityRemovesNodesFromBothLists() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node changedNode = PeerUtils.createNodeWithName("p");
		Mockito.when(context.connector.getInfo(context.refreshNodes.get(1)))
				.thenReturn(CompletableFuture.completedFuture(changedNode));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "c" }, new String[] { });
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(context.refreshNodes.get(1));
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(changedNode);
	}

	@Test
	public void refreshGetInfoIncompatibleNodeRemovesNodesFromBothLists() {
		// Arrange:
		final NodeVersionCheck versionCheck = Mockito.mock(NodeVersionCheck.class);
		final TestContext context = new TestContext(versionCheck);
		Mockito.when(versionCheck.check(Mockito.any(), Mockito.any())).thenReturn(true);

		final Node incompatibleNode = context.refreshNodes.get(1);
		incompatibleNode.setMetaData(new NodeMetaData("p", "a", new NodeVersion(1, 0, 0)));
		Mockito.when(versionCheck.check(context.localNode.getMetaData().getVersion(), new NodeVersion(1, 0, 0)))
				.thenReturn(false);

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "c" }, new String[] { });
		Mockito.verify(context.connector, Mockito.never()).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoChangeAddressUpdatesNodeEndpoint() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getInfo(context.refreshNodes.get(1)))
				.thenReturn(CompletableFuture.completedFuture(PeerUtils.createNodeWithHost("10.0.0.125", "b")));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();
		final Node updatedNode = context.nodes.findNodeByIdentity(new WeakNodeIdentity("b"));

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "b", "c" }, new String[] { });
		Assert.assertThat(updatedNode.getEndpoint(), IsEqual.equalTo(NodeEndpoint.fromHost("10.0.0.125")));
		Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(context.refreshNodes.get(1));
	}

	@Test
	public void refreshGetInfoChangeMetaDataUpdatesNodeMetaData() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getInfo(context.refreshNodes.get(1)))
				.thenReturn(CompletableFuture.completedFuture(new Node(
						new WeakNodeIdentity("b"),
						NodeEndpoint.fromHost("localhost"),
						new NodeMetaData("c-plat", "c-app", new NodeVersion(2, 1, 3)))));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();
		final Node updatedNode = context.nodes.findNodeByIdentity(new WeakNodeIdentity("b"));
		final NodeMetaData metaData = updatedNode.getMetaData();

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("c-plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("c-app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo(new NodeVersion(2, 1, 3)));
		Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(context.refreshNodes.get(1));
	}

	//endregion

	//region getKnownPeers calls

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

	//endregion

	//region getKnownPeers transitions / short-circuiting

	@Test
	public void refreshGetKnownPeersInfoTransientFailureMovesNodesToInactive() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getKnownPeers(context.refreshNodes.get(1)))
				.thenReturn(CompletableFuture.supplyAsync(() -> {
					throw new BusyPeerException("busy");
				}));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "c" }, new String[] { "b" });
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
		Mockito.when(context.connector.getKnownPeers(context.refreshNodes.get(1)))
				.thenReturn(CompletableFuture.supplyAsync(() -> { throw ex; }));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "c" }, new String[] { });

	}

	//endregion

	//region precedence / potential attacks

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
		NodeCollectionAssert.areNamesEquivalent(
				context.nodes,
				new String[] { "a", "c", "e" },
				new String[] { "b", "f" });
	}

	@Test
	public void refreshPreventsEvilNodeFromGettingGoodNodesDropped() {
		// this is similar to refreshGivesPrecedenceToFirstHandExperience
		// but is important to show the following attack is prevented:
		// evil node propagating mismatched identities for good nodes does not remove the good nodes

		// Arrange: set up 4 active nodes (3 pre-trusted)
		final TestContext context = new TestContext();
		context.refreshNodes.add(PeerUtils.createNodeWithName("d"));
		context.setBusyGetInfoForNode("c", DEFAULT_SLEEP);

		// Arrange: when the mock connector sees nodes w-x, it will trigger an identity change to a good node
		final List<Node> evilNodes = Arrays.asList(
				PeerUtils.createNodeWithHost("10.0.0.100", "w"),
				PeerUtils.createNodeWithHost("10.0.0.101", "x"),
				PeerUtils.createNodeWithHost("10.0.0.102", "y"),
				PeerUtils.createNodeWithHost("10.0.0.103", "z"));
		context.setKnownPeers(evilNodes);
		for (final Node evilNode : evilNodes) {
			final StringBuilder newNameBuilder = new StringBuilder();
			newNameBuilder.appendCodePoint(evilNode.getIdentity().getName().codePointAt(0) - 'w' + 'b');
			Mockito.when(context.connector.getInfo(evilNode))
					.thenReturn(CompletableFuture.completedFuture(
							PeerUtils.createNodeWithName(newNameBuilder.toString())));
		}

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		// - all good peers (a, b, d) that were directly communicated with are active
		// - the delayed inactive node (c) is inactive
		// - the impersonating bad nodes are not added
		NodeCollectionAssert.areNamesEquivalent(
				context.nodes,
				new String[] { "a", "b", "d" },
				new String[] { "c" });

		// Assert: the endpoints of good nodes were not changed
		for (final Node node : context.nodes.getAllNodes()) {
			Assert.assertThat(node.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.1"));
		}
	}

	@Test
	public void refreshResultForDirectNodeIgnoresChildNodeGetInfoResults() {
		// Arrange: set up 4 active nodes (3 pre-trusted)
		final TestContext context = new TestContext();
		context.refreshNodes.add(PeerUtils.createNodeWithName("d"));
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
		// - the unseen inactive peer (z) is inactive
		NodeCollectionAssert.areNamesEquivalent(
				context.nodes,
				new String[] { "a", "b", "d" },
				new String[] { "c", "z" });
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
		Mockito.verify(context.connector, Mockito.times(2)).getKnownPeers(Mockito.any());
		Mockito.verify(context.connector, Mockito.times(6)).getInfo(Mockito.any());
	}

	@Test
	public void refreshGetInfoIsBypassedForBlacklistedNodes() {
		// Arrange:
		final TestContext context = new TestContext();
		context.nodes.update(PeerUtils.createNodeWithName("a"), NodeStatus.FAILURE);
		context.nodes.update(PeerUtils.createNodeWithName("c"), NodeStatus.INACTIVE);

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "b" }, new String[] { });
		Mockito.verify(context.connector, Mockito.times(1)).getKnownPeers(Mockito.any());
		Mockito.verify(context.connector, Mockito.times(1)).getInfo(Mockito.any());
	}

	//endregion

	//region basic merging

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
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "b", "c", "y", "z" }, new String[] { });
	}

	@Test
	public void refreshDoesNotMergeInLocalNode() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getKnownPeers(Mockito.any()))
				.thenReturn(CompletableFuture.completedFuture(new SerializableList<>(Arrays.asList(context.localNode))));

		// Act:
		context.refresher.refresh(context.refreshNodes).join();

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(context.nodes, new String[] { "a", "b", "c" }, new String[] { });
	}

	//endregion

	//region async

	@Test
	public void refreshIsAsync() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setBusyGetInfoForNode("b", DEFAULT_SLEEP);

		// Act:
		final CompletableFuture future = context.refresher.refresh(context.refreshNodes);

		// Assert:
		Assert.assertThat(future.isDone(), IsEqual.equalTo(false));
	}

	//endregion

	private static PeerConnector mockPeerConnector() {
		final PeerConnector connector = Mockito.mock(PeerConnector.class);
		Mockito.when(connector.getInfo(Mockito.any()))
				.thenAnswer(i -> CompletableFuture.completedFuture((Node)i.getArguments()[0]));
		Mockito.when(connector.getKnownPeers(Mockito.any()))
				.thenAnswer(i -> CompletableFuture.completedFuture(new SerializableList<>(10)));
		return connector;
	}

	private static class TestContext {
		private final List<Node> refreshNodes = PeerUtils.createNodesWithNames("a", "b", "c");
		private final Node localNode = PeerUtils.createNodeWithName("l");
		private final NodeCollection nodes = new NodeCollection();
		private final PeerConnector connector = mockPeerConnector();
		private final NodeRefresher refresher;

		public TestContext() {
			this((l, r) -> true);
		}

		public TestContext(final NodeVersionCheck versionCheck) {
			this.refresher = new NodeRefresher(this.localNode, this.nodes, this.connector, versionCheck);
		}

		public void setBusyGetInfoForNode(final String name) {
			this.setBusyGetInfoForNode(name, 0);
		}

		public void setBusyGetInfoForNode(final String name, final int sleep) {
			final Node node = PeerUtils.createNodeWithName(name);
			Mockito.when(this.connector.getInfo(node))
					.thenReturn(CompletableFuture.supplyAsync(() -> {
						ExceptionUtils.propagateVoid(() -> Thread.sleep(sleep));
						throw new BusyPeerException("busy");
					}));
		}

		public void setInactiveGetInfoForNode(final String name) {
			final Node node = PeerUtils.createNodeWithName(name);
			Mockito.when(this.connector.getInfo(node))
					.thenReturn(CompletableFuture.supplyAsync(() -> { throw new InactivePeerException("inactive"); }));
		}

		public void setFatalGetInfoForNode(final String name) {
			final Node node = PeerUtils.createNodeWithName(name);
			Mockito.when(this.connector.getInfo(node))
					.thenReturn(CompletableFuture.supplyAsync(() -> { throw new FatalPeerException("fatal"); }));
		}

		public void setKnownPeers(final List<Node> nodes) {
			Mockito.when(this.connector.getKnownPeers(Mockito.any()))
					.thenAnswer(i -> CompletableFuture.completedFuture(new SerializableList<>(nodes)));
		}
	}
}