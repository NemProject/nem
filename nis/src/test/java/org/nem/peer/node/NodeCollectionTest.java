package org.nem.peer.node;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.peer.test.*;

import java.util.Iterator;

public class NodeCollectionTest {

	//region basic partitioning

	@Test
	public void multipleNodesArePartitionedCorrectly() {
		// Act:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { "A", "D", "F" }, new String[] { "B", "C" });
	}

	//endregion

	//region serialization

	@Test
	public void canRoundTripNodeCollection() {
		// Arrange:
		final NodeCollection originalNodes = createNodeCollectionWithMultipleNodes();

		// Assert:
		final NodeCollection nodes = new NodeCollection(org.nem.core.test.Utils.roundtripSerializableEntity(originalNodes, null));

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { "A", "D", "F" }, new String[] { "B", "C" });
	}

	//endregion

	//region getNodeStatus

	@Test
	public void getNodeStatusReturnsCorrectStatusForActiveNodes() {
		// Act:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Assert:
		Assert.assertThat(nodes.getNodeStatus(createNode("A")), IsEqual.equalTo(NodeStatus.ACTIVE));
		Assert.assertThat(nodes.getNodeStatus(createNode("D")), IsEqual.equalTo(NodeStatus.ACTIVE));
		Assert.assertThat(nodes.getNodeStatus(createNode("F")), IsEqual.equalTo(NodeStatus.ACTIVE));
	}

	@Test
	public void getNodeStatusReturnsCorrectStatusForInactiveNodes() {
		// Act:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Assert:
		Assert.assertThat(nodes.getNodeStatus(createNode("B")), IsEqual.equalTo(NodeStatus.INACTIVE));
		Assert.assertThat(nodes.getNodeStatus(createNode("C")), IsEqual.equalTo(NodeStatus.INACTIVE));
	}

	@Test
	public void getNodeStatusReturnsCorrectStatusForFailureNodes() {
		// Act:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Assert:
		Assert.assertThat(nodes.getNodeStatus(createNode("E")), IsEqual.equalTo(NodeStatus.FAILURE));
		Assert.assertThat(nodes.getNodeStatus(createNode("G")), IsEqual.equalTo(NodeStatus.FAILURE));
	}

	//endregion

	//region update

	/**
	 * NOTE: The update tests are using a node's port as its "hallmark" (in other words, nodes with the same port
	 * are deemed equal). The arePortsEquivalent validation ensures that the node we are checking is the one we expect.
	 * The "platform" is used as a non-identifying field that should be updated in the collection.
	 * The arePlatformsEquivalent ensures that it was updated.
	 */

	@Test(expected = NullPointerException.class)
	public void updateCannotAddNullNode() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(null, NodeStatus.ACTIVE);
	}

	@Test
	public void updateCanAddNewActiveNode() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(createNode("A"), NodeStatus.ACTIVE);

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { "A" }, new String[] { });
		NodeCollectionAssert.arePortsEquivalent(nodes, new Integer[] { (int)'A' }, new Integer[] { });
	}

	@Test
	public void updateCanAddNewInactiveNode() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(createNode("A"), NodeStatus.INACTIVE);

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { }, new String[] { "A" });
		NodeCollectionAssert.arePortsEquivalent(nodes, new Integer[] { }, new Integer[] { (int)'A' });
	}

	@Test
	public void updateDoesNotAddNewFailureNode() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();

		// Act:
		nodes.update(createNode("A"), NodeStatus.FAILURE);

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { }, new String[] { });
	}

	@Test
	public void updateCanUpdateActiveNode() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();
		nodes.update(createNode("A"), NodeStatus.ACTIVE);

		// Act:
		nodes.update(createNode("B", 'A'), NodeStatus.ACTIVE);

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { "B" }, new String[] { });
		NodeCollectionAssert.arePortsEquivalent(nodes, new Integer[] { (int)'A' }, new Integer[] { });
	}

	@Test
	public void updateCanUpdateActiveNodeAsInactiveNode() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();
		nodes.update(createNode("A"), NodeStatus.ACTIVE);

		// Act:
		nodes.update(createNode("B", 'A'), NodeStatus.INACTIVE);

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { }, new String[] { "B" });
		NodeCollectionAssert.arePortsEquivalent(nodes, new Integer[] { }, new Integer[] { (int)'A' });
	}

	@Test
	public void updateCanUpdateInactiveNodeAsActiveNode() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();
		nodes.update(createNode("A"), NodeStatus.INACTIVE);

		// Act:
		nodes.update(createNode("B", 'A'), NodeStatus.ACTIVE);

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { "B" }, new String[] { });
		NodeCollectionAssert.arePortsEquivalent(nodes, new Integer[] { (int)'A' }, new Integer[] { });
	}

	@Test
	public void updateCanUpdateInactiveNode() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();
		nodes.update(createNode("A"), NodeStatus.INACTIVE);

		// Act:
		nodes.update(createNode("B", 'A'), NodeStatus.INACTIVE);

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { }, new String[] { "B" });
		NodeCollectionAssert.arePortsEquivalent(nodes, new Integer[] { }, new Integer[] { (int)'A' });
	}

	@Test
	public void updateOnlyUpdatesMatchingNode() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Act:
		nodes.update(createNode("Z", 'D'), NodeStatus.INACTIVE);

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { "A", "F" }, new String[] { "B", "C", "Z" });
		NodeCollectionAssert.arePortsEquivalent(nodes, new Integer[] { (int)'A', (int)'F' }, new Integer[] { (int)'B', (int)'C', (int)'D' });
	}

	//endregion

	//region concurrency

	@Test
	public void getActiveNodesIsConcurrencySafe() {
		// Arrange: partially iterate the set
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();
		Iterator<Node> it = nodes.getActiveNodes().iterator();
		it.next();

		// Act: update the set and resume the iteration
		nodes.update(createNode("Z"), NodeStatus.ACTIVE);
		it.next();

		// Assert: no ConcurrentModificationException is thrown
	}

	@Test
	public void getInactiveNodesIsConcurrencySafe() {
		// Arrange: partially iterate the set
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();
		Iterator<Node> it = nodes.getInactiveNodes().iterator();
		it.next();

		// Act: update the set and resume the iteration
		nodes.update(createNode("Z"), NodeStatus.INACTIVE);
		it.next();

		// Assert: no ConcurrentModificationException is thrown
	}

	//endregion

	//region collections

	@Test
	public void nodeCollectionsContainAllExpectedNodes() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

		// Assert:
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { "A", "D", "F" }, new String[] { "B", "C" });
		NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[] { "A", "B", "C", "D", "F" });
	}

	//endregion

	//region findNodeByEndpoint

	@Test
	public void findNodeByEndpointReturnsActiveNodeMatchingEndpoint() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionForFindNodeByEndpointTests();

		// Act: 
		final Node node = nodes.findNodeByEndpoint(new NodeEndpoint("http", "37.123.25.5", 7890));
		
		// Assert:
		Assert.assertThat(node, IsEqual.equalTo(createNode("A", "37.123.25.5", 7890)));
	}

	@Test
	public void findNodeByEndpointReturnsInactiveNodeMatchingEndpoint() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionForFindNodeByEndpointTests();

		// Act:
		final Node node = nodes.findNodeByEndpoint(new NodeEndpoint("http", "38.183.85.5", 7890));

		// Assert:
		Assert.assertThat(node, IsEqual.equalTo(createNode("A", "38.183.85.5", 7890)));
	}

	@Test
	public void findNodeByEndpointReturnsNullIfNoNodeMatchesEndpoint() {
		// Arrange:
		final NodeCollection nodes = createNodeCollectionForFindNodeByEndpointTests();

		// Act:
		final Node node = nodes.findNodeByEndpoint(new NodeEndpoint("http", "37.121.27.7", 7891));

		// Assert:
		Assert.assertThat(node, IsNull.nullValue());
	}

	private static NodeCollection createNodeCollectionForFindNodeByEndpointTests() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();
		nodes.update(createNode("A", "37.128.23.2", 7890), NodeStatus.ACTIVE);
		nodes.update(createNode("A", "37.123.25.5", 7890), NodeStatus.ACTIVE);
		nodes.update(createNode("A", "37.121.27.7", 7890), NodeStatus.ACTIVE);
		nodes.update(createNode("A", "38.188.83.2", 7890), NodeStatus.INACTIVE);
		nodes.update(createNode("A", "38.183.85.5", 7890), NodeStatus.INACTIVE);
		nodes.update(createNode("A", "38.181.87.7", 7890), NodeStatus.INACTIVE);
		return nodes;
	}

	//endregion

	private static Node createNode(final String platform) {
		// Arrange:
		return createNode(platform, platform.charAt(0));
	}

	private static Node createNode(final String platform, final char port) {
		// Arrange:
		return new Node(new NodeEndpoint("http", "localhost", port), platform, "FooBar");
	}

	private static Node createNode(final String platform, final String host, final int port) {
		// Arrange:
		return new Node(new NodeEndpoint("http", host, port), platform, "FooBar");
	}

	private static NodeCollection createNodeCollectionWithMultipleNodes() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();
		nodes.update(createNode("A"), NodeStatus.ACTIVE);
		nodes.update(createNode("B"), NodeStatus.INACTIVE);
		nodes.update(createNode("C"), NodeStatus.INACTIVE);
		nodes.update(createNode("D"), NodeStatus.ACTIVE);
		nodes.update(createNode("E"), NodeStatus.FAILURE);
		nodes.update(createNode("F"), NodeStatus.ACTIVE);
		return nodes;
	}
}