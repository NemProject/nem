package org.nem.core.node;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.test.*;

import java.util.*;
import java.util.stream.Collectors;

public class NodeCollectionTest {

	//region constructor

	@Test
	public void collectionIsInitiallyEmpty() {
		// Act:
		final NodeCollection nodes = new NodeCollection();

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getActiveNodes().size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getBusyNodes().size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getAllNodes().size(), IsEqual.equalTo(0));
		for (final NodeStatus status : NodeStatus.values()) {
			Assert.assertThat(nodes.getNodes(status).size(), IsEqual.equalTo(0));
		}
	}

	//endregion

	//region adding single node

	//region counts

	@Test
	public void activeNodeCanBeAddedToCollectionAndUpdatesCounts() {
		// Assert:
		assertNodeWithStatusCanBeAddedToCollectionAndUpdatesCounts(NodeStatus.ACTIVE);
	}

	@Test
	public void busyNodeCanBeAddedToCollectionAndUpdatesCounts() {
		// Assert:
		assertNodeWithStatusCanBeAddedToCollectionAndUpdatesCounts(NodeStatus.BUSY);
	}

	@Test
	public void inactiveNodeCanBeAddedToCollectionAndUpdatesCounts() {
		// Assert:
		assertNodeWithStatusCanBeAddedToCollectionAndUpdatesCounts(NodeStatus.INACTIVE);
	}

	@Test
	public void failureNodeCanBeAddedToCollectionAndUpdatesCounts() {
		// Assert:
		assertNodeWithStatusCanBeAddedToCollectionAndUpdatesCounts(NodeStatus.FAILURE);
	}

	private static void assertNodeWithStatusCanBeAddedToCollectionAndUpdatesCounts(final NodeStatus expectedStatus) {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(NodeUtils.createNodeWithName("X"), expectedStatus);

		// Assert:
		final boolean isActiveOrBusy = NodeStatus.ACTIVE == expectedStatus || NodeStatus.BUSY == expectedStatus;
		Assert.assertThat(nodes.size(), IsEqual.equalTo(1));
		Assert.assertThat(nodes.getActiveNodes().size(), IsEqual.equalTo(NodeStatus.ACTIVE == expectedStatus ? 1 : 0));
		Assert.assertThat(nodes.getBusyNodes().size(), IsEqual.equalTo(NodeStatus.BUSY == expectedStatus ? 1 : 0));
		Assert.assertThat(nodes.getAllNodes().size(), IsEqual.equalTo(isActiveOrBusy ? 1 : 0));
		for (final NodeStatus status : NodeStatus.values()) {
			Assert.assertThat(nodes.getNodes(status).size(), IsEqual.equalTo(status == expectedStatus ? 1 : 0));
		}
	}

	@Test
	public void unknownNodeCannotBeAddedToCollection() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(NodeUtils.createNodeWithName("X"), NodeStatus.UNKNOWN);

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getActiveNodes().size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getBusyNodes().size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getAllNodes().size(), IsEqual.equalTo(0));
		for (final NodeStatus status : NodeStatus.values()) {
			Assert.assertThat(nodes.getNodes(status).size(), IsEqual.equalTo(0));
		}
	}

	//endregion

	//region collections

	@Test
	public void activeNodeCanBeAddedToCollectionAndExposedBySubCollections() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("X");
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(node, NodeStatus.ACTIVE);

		// Assert:
		final Collection<Node> expectedNodes = Collections.singletonList(node);
		Assert.assertThat(nodes.getActiveNodes(), IsEquivalent.equivalentTo(expectedNodes));
		Assert.assertThat(nodes.getAllNodes(), IsEquivalent.equivalentTo(expectedNodes));
		Assert.assertThat(nodes.getNodes(NodeStatus.ACTIVE), IsEquivalent.equivalentTo(expectedNodes));
	}

	@Test
	public void busyNodeCanBeAddedToCollectionAndExposedBySubCollections() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("X");
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(node, NodeStatus.BUSY);

		// Assert:
		final Collection<Node> expectedNodes = Collections.singletonList(node);
		Assert.assertThat(nodes.getBusyNodes(), IsEquivalent.equivalentTo(expectedNodes));
		Assert.assertThat(nodes.getAllNodes(), IsEquivalent.equivalentTo(expectedNodes));
		Assert.assertThat(nodes.getNodes(NodeStatus.BUSY), IsEquivalent.equivalentTo(expectedNodes));
	}

	@Test
	public void inactiveNodeCanBeAddedToCollectionAndExposedBySubCollections() {
		// Assert:
		assertNodeWithStatusCanBeAddedToCollectionAndExposedByCollections(NodeStatus.INACTIVE);
	}

	@Test
	public void failureNodeCanBeAddedToCollectionAndExposedBySubCollections() {
		// Assert:
		assertNodeWithStatusCanBeAddedToCollectionAndExposedByCollections(NodeStatus.FAILURE);
	}

	private static void assertNodeWithStatusCanBeAddedToCollectionAndExposedByCollections(final NodeStatus status) {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("X");
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(node, status);

		// Assert:
		Assert.assertThat(nodes.getNodes(status), IsEquivalent.equivalentTo(Collections.singletonList(node)));
	}

	//endregion

	//region getNodeStatus

	@Test
	public void getNodeStatusReturnsCorrectStatusForActiveNode() {
		// Assert:
		assertGetNodeStatusReturnsCorrectStatusForNodeWithStatus(NodeStatus.ACTIVE);
	}

	@Test
	public void getNodeStatusReturnsCorrectStatusForBusyNode() {
		// Assert:
		assertGetNodeStatusReturnsCorrectStatusForNodeWithStatus(NodeStatus.BUSY);
	}

	@Test
	public void getNodeStatusReturnsCorrectStatusForInactiveNode() {
		// Assert:
		assertGetNodeStatusReturnsCorrectStatusForNodeWithStatus(NodeStatus.INACTIVE);
	}

	@Test
	public void getNodeStatusReturnsCorrectStatusForFailureNode() {
		// Assert:
		assertGetNodeStatusReturnsCorrectStatusForNodeWithStatus(NodeStatus.FAILURE);
	}

	private static void assertGetNodeStatusReturnsCorrectStatusForNodeWithStatus(final NodeStatus status) {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("X");
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(node, status);

		// Assert:
		Assert.assertThat(nodes.getNodeStatus(node), IsEqual.equalTo(status));
	}

	@Test
	public void getNodeStatusReturnsCorrectStatusForOtherNode() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("X");
		final NodeCollection nodes = new NodeCollection();

		// Assert:
		Assert.assertThat(nodes.getNodeStatus(node), IsEqual.equalTo(NodeStatus.UNKNOWN));
	}

	//endregion

	//region findNodeByEndpoint

	@Test
	public void findNodeByEndpointReturnsActiveNodeMatchingEndpoint() {
		// Assert:
		assertFindNodeByEndpointReturnsNodeWithStatusMatchingEndpoint(NodeStatus.ACTIVE);
	}

	@Test
	public void findNodeByEndpointReturnsBusyNodeMatchingEndpoint() {
		// Assert:
		assertFindNodeByEndpointReturnsNodeWithStatusMatchingEndpoint(NodeStatus.BUSY);
	}

	private static void assertFindNodeByEndpointReturnsNodeWithStatusMatchingEndpoint(final NodeStatus status) {
		// Arrange:
		final Node node = NodeUtils.createNodeWithHost("10.0.0.1", "X");
		final NodeCollection nodes = new NodeCollection();
		nodes.update(node, status);

		// Act:
		final Node resultNode = nodes.findNodeByEndpoint(NodeEndpoint.fromHost("10.0.0.1"));

		// Assert:
		Assert.assertThat(resultNode, IsEqual.equalTo(node));
	}

	@Test
	public void findNodeByEndpointDoesNotReturnInactiveNodeMatchingEndpoint() {
		// Assert:
		assertFindNodeByEndpointDoesNotReturnNodeWithStatusMatchingEndpoint(NodeStatus.INACTIVE);
	}

	@Test
	public void findNodeByEndpointDoesNotReturnFailureNodeMatchingEndpoint() {
		// Assert:
		assertFindNodeByEndpointDoesNotReturnNodeWithStatusMatchingEndpoint(NodeStatus.FAILURE);
	}

	private static void assertFindNodeByEndpointDoesNotReturnNodeWithStatusMatchingEndpoint(final NodeStatus status) {
		// Arrange:
		final Node node = NodeUtils.createNodeWithHost("10.0.0.1", "X");
		final NodeCollection nodes = new NodeCollection();
		nodes.update(node, status);

		// Act:
		final Node resultNode = nodes.findNodeByEndpoint(NodeEndpoint.fromHost("10.0.0.1"));

		// Assert:
		Assert.assertThat(resultNode, IsNull.nullValue());
	}

	@Test
	public void findNodeByEndpointReturnsNullIfNoNodeMatchesEndpoint() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();

		// Act:
		final Node resultNode = nodes.findNodeByEndpoint(NodeEndpoint.fromHost("10.0.0.1"));

		// Assert:
		Assert.assertThat(resultNode, IsNull.nullValue());
	}

	//endregion

	//region findNodeByIdentity

	@Test
	public void findNodeByIdentityReturnsActiveNodeMatchingIdentity() {
		// Assert:
		assertFindNodeByIdentityReturnsNodeWithStatusMatchingIdentity(NodeStatus.ACTIVE);
	}

	@Test
	public void findNodeByIdentityReturnsBusyNodeMatchingIdentity() {
		// Assert:
		assertFindNodeByIdentityReturnsNodeWithStatusMatchingIdentity(NodeStatus.BUSY);
	}

	private static void assertFindNodeByIdentityReturnsNodeWithStatusMatchingIdentity(final NodeStatus status) {
		// Arrange:
		final Node node = NodeUtils.createNodeWithHost("10.0.0.1", "X");
		final NodeCollection nodes = new NodeCollection();
		nodes.update(node, status);

		// Act:
		final Node resultNode = nodes.findNodeByIdentity(new WeakNodeIdentity("X"));

		// Assert:
		Assert.assertThat(resultNode, IsEqual.equalTo(node));
	}

	@Test
	public void findNodeByIdentityDoesNotReturnInactiveNodeMatchingIdentity() {
		// Assert:
		assertFindNodeByIdentityDoesNotReturnNodeWithStatusMatchingIdentity(NodeStatus.INACTIVE);
	}

	@Test
	public void findNodeByIdentityDoesNotReturnFailureNodeMatchingIdentity() {
		// Assert:
		assertFindNodeByIdentityDoesNotReturnNodeWithStatusMatchingIdentity(NodeStatus.FAILURE);
	}

	private static void assertFindNodeByIdentityDoesNotReturnNodeWithStatusMatchingIdentity(final NodeStatus status) {
		// Arrange:
		final Node node = NodeUtils.createNodeWithHost("10.0.0.1", "X");
		final NodeCollection nodes = new NodeCollection();
		nodes.update(node, status);

		// Act:
		final Node resultNode = nodes.findNodeByIdentity(new WeakNodeIdentity("X"));

		// Assert:
		Assert.assertThat(resultNode, IsNull.nullValue());
	}

	@Test
	public void findNodeByIdentityReturnsNullIfNoNodeMatchesIdentity() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();

		// Act:
		final Node resultNode = nodes.findNodeByIdentity(new WeakNodeIdentity("X"));

		// Assert:
		Assert.assertThat(resultNode, IsNull.nullValue());
	}

	//endregion

	//region isNodeBlacklisted

	@Test
	public void activeNodeIsNotBlacklisted() {
		// Assert:
		assertNodeWithStatusIsBlacklisted(NodeStatus.ACTIVE, false);
	}

	@Test
	public void busyNodeIsNotBlacklisted() {
		// Assert:
		assertNodeWithStatusIsBlacklisted(NodeStatus.BUSY, false);
	}

	@Test
	public void inactiveNodeIsBlacklisted() {
		// Assert:
		assertNodeWithStatusIsBlacklisted(NodeStatus.INACTIVE, true);
	}

	@Test
	public void failureNodeIsBlacklisted() {
		// Assert:
		assertNodeWithStatusIsBlacklisted(NodeStatus.FAILURE, true);
	}

	private static void assertNodeWithStatusIsBlacklisted(final NodeStatus status, final boolean isBlacklisted) {
		// Arrange:
		final Node node = NodeUtils.createNodeWithHost("10.0.0.1", "X");
		final NodeCollection nodes = new NodeCollection();
		nodes.update(node, status);

		// Assert:
		Assert.assertThat(nodes.isNodeBlacklisted(node), IsEqual.equalTo(isBlacklisted));
	}

	@Test
	public void unknownNodeIsNotBlacklisted() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithHost("10.0.0.1", "X");
		final NodeCollection nodes = new NodeCollection();

		// Assert:
		Assert.assertThat(nodes.isNodeBlacklisted(node), IsEqual.equalTo(false));
	}

	//endregion

	//endregion

	//region multiple node tests

	@Test
	public void sizeIsTheCountOfAllNodesInAllCollections() {
		// Act:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(8));
	}

	@Test
	public void multipleNodesArePartitionedCorrectly() {
		// Act:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Assert:
		assertMultipleNodesCollection(nodes);
	}

	@Test
	public void canRoundTripNodeCollection() {
		// Arrange:
		final NodeCollection originalNodes = createNodeCollectionWithMultipleNodes();

		// Assert:
		final NodeCollection nodes = new NodeCollection(org.nem.core.test.Utils.roundtripSerializableEntity(originalNodes, null));

		// Assert:
		assertMultipleNodesCollection(nodes);
	}

	//endregion

	//region update edge cases / transitions

	@Test(expected = NullPointerException.class)
	public void updateCannotAddNullNode() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(null, NodeStatus.ACTIVE);
	}

	@Test
	public void updateRemovesNodeIfStatusChangesToUnknown() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("X");
		final NodeCollection nodes = new NodeCollection();
		nodes.update(node, NodeStatus.ACTIVE);

		// Act:
		nodes.update(node, NodeStatus.UNKNOWN);

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(0));
	}

	@Test
	public void updateCanUpdateNodeEndpointWithoutStatusChange() {
		// Assert:
		updateCanUpdateNodeEndpoint(NodeStatus.ACTIVE, NodeStatus.ACTIVE);
	}

	@Test
	public void updateCanUpdateNodeEndpointWithStatusChange() {
		// Assert:
		updateCanUpdateNodeEndpoint(NodeStatus.ACTIVE, NodeStatus.BUSY);
	}

	private static void updateCanUpdateNodeEndpoint(final NodeStatus originalStatus, final NodeStatus endingStatus) {
		// Arrange:
		final Node node1 = NodeUtils.createNodeWithHost("10.0.0.1", "X");
		final Node node2 = NodeUtils.createNodeWithHost("10.0.0.2", "X");
		final NodeCollection nodes = new NodeCollection();
		nodes.update(node1, originalStatus);

		// Act:
		nodes.update(node2, endingStatus);

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(1));
		Assert.assertThat(nodes.getNodes(endingStatus).size(), IsEqual.equalTo(1));

		final Node collectionNode = nodes.getNodes(endingStatus).iterator().next();
		Assert.assertThat(collectionNode.getIdentity().getName(), IsEqual.equalTo("X"));
		Assert.assertThat(collectionNode.getEndpoint(), IsEqual.equalTo(NodeEndpoint.fromHost("10.0.0.2")));
	}

	@Test
	public void updateCanUpdateNodeMetaDataWithoutStatusChange() {
		// Assert:
		updateCanUpdateNodeMetaData(NodeStatus.ACTIVE, NodeStatus.ACTIVE);
	}

	@Test
	public void updateCanUpdateNodeMetaDataWithStatusChange() {
		// Assert:
		updateCanUpdateNodeMetaData(NodeStatus.ACTIVE, NodeStatus.BUSY);
	}

	private static void updateCanUpdateNodeMetaData(final NodeStatus originalStatus, final NodeStatus endingStatus) {
		// Arrange:
		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final Node node1 = new Node(
				identity,
				NodeEndpoint.fromHost("10.0.0.1"),
				new NodeMetaData("plat", "app"));
		final Node node2 = new Node(
				identity,
				NodeEndpoint.fromHost("10.0.0.3"),
				new NodeMetaData("plat2", "app2"));
		final NodeCollection nodes = new NodeCollection();
		nodes.update(node1, originalStatus);

		// Act:
		nodes.update(node2, endingStatus);

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(1));
		Assert.assertThat(nodes.getNodes(endingStatus).size(), IsEqual.equalTo(1));

		final Node collectionNode = nodes.getNodes(endingStatus).iterator().next();
		Assert.assertThat(collectionNode.getIdentity(), IsEqual.equalTo(identity));
		Assert.assertThat(collectionNode.getMetaData(), IsEqual.equalTo(node2.getMetaData()));
	}

	@Test
	public void updateOnlyUpdatesMatchingNode() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("A2");
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Act:
		nodes.update(node, NodeStatus.BUSY);

		// Assert: A2 moved from active to busy
		Assert.assertThat(nodes.size(), IsEqual.equalTo(8));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.ACTIVE)), IsEquivalent.equivalentTo(Arrays.asList("A1", "A3")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.BUSY)), IsEquivalent.equivalentTo(Arrays.asList("B1", "B2", "A2")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.INACTIVE)), IsEquivalent.equivalentTo(Collections.singletonList("I1")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.FAILURE)), IsEquivalent.equivalentTo(Arrays.asList("F1", "F2")));
		Assert.assertThat(nodes.getNodes(NodeStatus.UNKNOWN).size(), IsEqual.equalTo(0));
	}

	//endregion

	//region concurrency

	@Test
	public void getActiveNodesIsConcurrencySafe() {
		// Arrange: partially iterate the set
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();
		final Iterator<Node> it = nodes.getActiveNodes().iterator();
		it.next();

		// Act: update the set and resume the iteration
		nodes.update(NodeUtils.createNodeWithName("Z"), NodeStatus.ACTIVE);
		it.next();

		// Assert: no ConcurrentModificationException is thrown
	}

	@Test
	public void getInactiveNodesIsConcurrencySafe() {
		// Arrange: partially iterate the set
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();
		final Iterator<Node> it = nodes.getBusyNodes().iterator();
		it.next();

		// Act: update the set and resume the iteration
		nodes.update(NodeUtils.createNodeWithName("Z"), NodeStatus.BUSY);
		it.next();

		// Assert: no ConcurrentModificationException is thrown
	}

	//endregion

	//region prune

	@Test
	public void pruneDoesNotHaveAnySideEffectInitially() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Act:
		nodes.prune(); // candidate: { B1, B2, I1, F1, F2 }

		// Assert:
		assertMultipleNodesCollection(nodes);
	}

	@Test
	public void pruneRemovesNodesThatHaveNotBeenActiveSinceLastCall() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Act:
		nodes.prune(); // candidate: { B1, B2, I1, F1, F2 }
		nodes.prune(); // removed: { B1, B2, I1, F1, F2 }

		// Assert:
		assertMultipleNodesCollectionAfterPruning(nodes);
	}

	@Test
	public void pruneDoesNotRemovePruneCandidateNodesThatBecomeActiveSinceLastCall() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Act:
		nodes.prune(); // candidate: { B1, B2, I1, F1, F2 }
		nodes.update(NodeUtils.createNodeWithName("B2"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("F1"), NodeStatus.ACTIVE);
		nodes.prune(); // removed: { B1, I1, F2 }

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(5));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.ACTIVE)), IsEquivalent.equivalentTo(Arrays.asList("A1", "A2", "A3", "B2", "F1")));
		Assert.assertThat(nodes.getNodes(NodeStatus.BUSY).size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getNodes(NodeStatus.INACTIVE).size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getNodes(NodeStatus.FAILURE).size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getNodes(NodeStatus.UNKNOWN).size(), IsEqual.equalTo(0));
	}

	@Test
	public void pruneDoesRemovePruneCandidateNodesThatBecomeBusySinceLastCall() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Act:
		nodes.prune(); // candidate: { B1, B2, I1, F1, F2 }
		nodes.update(NodeUtils.createNodeWithName("F1"), NodeStatus.BUSY);
		nodes.prune(); // removed: { B1, B2, I1, F1, F2 }

		// Assert:
		assertMultipleNodesCollectionAfterPruning(nodes);
	}

	@Test
	public void pruneDoesRemovePruneCandidateNodesThatBecomeInactiveSinceLastCall() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Act:
		nodes.prune(); // candidate: { B1, B2, I1, F1, F2 }
		nodes.update(NodeUtils.createNodeWithName("F1"), NodeStatus.INACTIVE);
		nodes.prune(); // removed: { B1, B2, I1, F1, F2 }

		// Assert:
		assertMultipleNodesCollectionAfterPruning(nodes);
	}

	@Test
	public void pruneDoesRemovePruneCandidateNodesThatBecomeFailureSinceLastCall() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Act:
		nodes.prune(); // candidate: { B1, B2, I1, F1, F2 }
		nodes.update(NodeUtils.createNodeWithName("I1"), NodeStatus.FAILURE);
		nodes.prune(); // removed: { B1, B2, I1, F1, F2 }

		// Assert:
		assertMultipleNodesCollectionAfterPruning(nodes);
	}

	@Test
	public void pruneDoesNotRemoveNodesThatHaveCycledBetweenActiveAndNonActiveSinceLastCall() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Act:
		nodes.prune(); // candidate: { B1, B2, I1, F1, F2 }
		nodes.update(NodeUtils.createNodeWithName("F1"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("F1"), NodeStatus.INACTIVE);

		nodes.update(NodeUtils.createNodeWithName("B2"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("B2"), NodeStatus.FAILURE);

		nodes.update(NodeUtils.createNodeWithName("I1"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("I1"), NodeStatus.BUSY);
		nodes.prune(); // removed: { B1, F2 }

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(6));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.ACTIVE)), IsEquivalent.equivalentTo(Arrays.asList("A1", "A2", "A3")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.BUSY)), IsEquivalent.equivalentTo(Collections.singletonList("I1")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.INACTIVE)), IsEquivalent.equivalentTo(Collections.singletonList("F1")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.FAILURE)), IsEquivalent.equivalentTo(Collections.singletonList("B2")));
		Assert.assertThat(nodes.getNodes(NodeStatus.UNKNOWN).size(), IsEqual.equalTo(0));
	}

	@Test
	public void pruneOnlyRemembersMostRecentSnapshot() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Act:
		nodes.prune(); // candidate: { B1, B2, I1, F1, F2 }
		nodes.update(NodeUtils.createNodeWithName("B1"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("I1"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("F2"), NodeStatus.ACTIVE);
		nodes.prune(); // candidate: { B2, F1 }
		nodes.update(NodeUtils.createNodeWithName("B1"), NodeStatus.BUSY);
		nodes.update(NodeUtils.createNodeWithName("I1"), NodeStatus.INACTIVE);
		nodes.update(NodeUtils.createNodeWithName("F2"), NodeStatus.FAILURE);
		nodes.prune(); // candidate: { B1, I1, F2 }, pruned: { B2, F1 }

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(6));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.ACTIVE)), IsEquivalent.equivalentTo(Arrays.asList("A1", "A2", "A3")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.BUSY)), IsEquivalent.equivalentTo(Collections.singletonList("B1")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.INACTIVE)), IsEquivalent.equivalentTo(Collections.singletonList("I1")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.FAILURE)), IsEquivalent.equivalentTo(Collections.singletonList("F2")));
		Assert.assertThat(nodes.getNodes(NodeStatus.UNKNOWN).size(), IsEqual.equalTo(0));
	}

	//endregion

	//region equals / hashCode

	private static final Map<String, NodeCollection> DESC_TO_NODES_MAP = new HashMap<String, NodeCollection>() {
		{
			this.put("default", createNodeCollection(new String[] { "A", "F", "P" }, new String[] { "B", "Y" }));
			this.put("diff-active", createNodeCollection(new String[] { "A", "F", "P", "Z" }, new String[] { "B", "Y" }));
			this.put("diff-inactive", createNodeCollection(new String[] { "A", "F", "P" }, new String[] { "B" }));
			this.put("diff-status", createNodeCollection(new String[] { "A", "F", "Y" }, new String[] { "B", "P" }));

			final NodeCollection other = createNodeCollection(new String[] { "A", "F", "P" }, new String[] { "B", "Y" });
			other.update(NodeUtils.createNodeWithName("X"), NodeStatus.INACTIVE);
			this.put("diff-other", other);
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeCollection nodes = createNodeCollection(new String[] { "A", "F", "P" }, new String[] { "B", "Y" });

		// Assert:
		Assert.assertThat(DESC_TO_NODES_MAP.get("default"), IsEqual.equalTo(nodes));
		Assert.assertThat(DESC_TO_NODES_MAP.get("diff-active"), IsNot.not(IsEqual.equalTo(nodes)));
		Assert.assertThat(DESC_TO_NODES_MAP.get("diff-inactive"), IsNot.not(IsEqual.equalTo(nodes)));
		Assert.assertThat(DESC_TO_NODES_MAP.get("diff-status"), IsNot.not(IsEqual.equalTo(nodes)));
		Assert.assertThat(DESC_TO_NODES_MAP.get("diff-other"), IsNot.not(IsEqual.equalTo(nodes)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(nodes)));
		Assert.assertThat(new String[] { "A", "F", "Y" }, IsNot.not(IsEqual.equalTo((Object)nodes)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeCollection nodes = createNodeCollection(new String[] { "A", "F", "P" }, new String[] { "B", "Y" });
		final int hashCode = nodes.hashCode();

		// Assert:
		Assert.assertThat(DESC_TO_NODES_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(DESC_TO_NODES_MAP.get("diff-active").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_NODES_MAP.get("diff-inactive").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_NODES_MAP.get("diff-status").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_NODES_MAP.get("diff-other").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	private static NodeCollection createNodeCollection(
			final String[] activeNodeNames,
			final String[] inactiveNodeNames) {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();
		for (final String nodeName : activeNodeNames) {
			nodes.update(NodeUtils.createNodeWithName(nodeName), NodeStatus.ACTIVE);
		}

		for (final String nodeName : inactiveNodeNames) {
			nodes.update(NodeUtils.createNodeWithName(nodeName), NodeStatus.BUSY);
		}

		return nodes;
	}

	//endregion

	private static NodeCollection createNodeCollectionWithMultipleNodes() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();
		nodes.update(NodeUtils.createNodeWithName("A1"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("B1"), NodeStatus.BUSY);
		nodes.update(NodeUtils.createNodeWithName("B2"), NodeStatus.BUSY);
		nodes.update(NodeUtils.createNodeWithName("A2"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("F1"), NodeStatus.FAILURE);
		nodes.update(NodeUtils.createNodeWithName("A3"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("I1"), NodeStatus.INACTIVE);
		nodes.update(NodeUtils.createNodeWithName("F2"), NodeStatus.FAILURE);
		nodes.update(NodeUtils.createNodeWithName("U1"), NodeStatus.UNKNOWN);
		return nodes;
	}

	private static void assertMultipleNodesCollection(final NodeCollection nodes) {
		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(8));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.ACTIVE)), IsEquivalent.equivalentTo(Arrays.asList("A1", "A2", "A3")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.BUSY)), IsEquivalent.equivalentTo(Arrays.asList("B1", "B2")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.INACTIVE)), IsEquivalent.equivalentTo(Collections.singletonList("I1")));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.FAILURE)), IsEquivalent.equivalentTo(Arrays.asList("F1", "F2")));
		Assert.assertThat(nodes.getNodes(NodeStatus.UNKNOWN).size(), IsEqual.equalTo(0));
	}

	private static void assertMultipleNodesCollectionAfterPruning(final NodeCollection nodes) {
		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(3));
		Assert.assertThat(getNames(nodes.getNodes(NodeStatus.ACTIVE)), IsEquivalent.equivalentTo(Arrays.asList("A1", "A2", "A3")));
		Assert.assertThat(nodes.getNodes(NodeStatus.BUSY).size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getNodes(NodeStatus.INACTIVE).size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getNodes(NodeStatus.FAILURE).size(), IsEqual.equalTo(0));
		Assert.assertThat(nodes.getNodes(NodeStatus.UNKNOWN).size(), IsEqual.equalTo(0));
	}

	private static List<String> getNames(final Collection<Node> nodes) {
		return nodes.stream()
				.map(node -> node.getIdentity().getName())
				.collect(Collectors.toList());
	}
}