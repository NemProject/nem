package org.nem.peer.v2;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.util.*;

public class NodeStatusDemuxTest {

    //region basic partitioning

    @Test
    public void activeNodeIsPlacedInActiveList() {
        // Arrange:
        assertStatusListAssignment(NodeStatus.ACTIVE, 1, 0);
    }

    @Test
    public void inactiveNodeIsPlacedInInactiveList() {
        // Arrange:
        assertStatusListAssignment(NodeStatus.INACTIVE, 0, 1);
    }

    @Test
    public void failedNodeIsNotPlacedInAnyList() {
        // Assert:
        assertStatusListAssignment(NodeStatus.FAILURE, 0, 0);
    }

     @Test
    public void inputListWithMultipleNodesIsPartitionedCorrectly() {
        // Arrange:
        final List<Node> nodes = new ArrayList<>();
        nodes.add(createNode(NodeStatus.ACTIVE, "A"));
        nodes.add(createNode(NodeStatus.INACTIVE, "B"));
        nodes.add(createNode(NodeStatus.INACTIVE, "C"));
        nodes.add(createNode(NodeStatus.ACTIVE, "D"));
        nodes.add(createNode(NodeStatus.FAILURE, "E"));
        nodes.add(createNode(NodeStatus.ACTIVE, "F"));

         // Act:
         final NodeStatusDemux demux = new NodeStatusDemux(nodes);

         // Assert:
         assertStatusListNodes(demux, new String[]{ "A", "D", "F" }, new String[]{ "B", "C" });
    }

    private static List<String> getPlatforms(final Collection<NodeInfo> nodes) {
        final List<String> platforms = new ArrayList<>();
        for (final NodeInfo node : nodes)
            platforms.add(node.getPlatform());
        return platforms;
    }

    private static void assertStatusListAssignment(
        final NodeStatus status,
        final int expectedNumActiveNodes,
        final int expectedNumInactiveNodes) {
        // Arrange:
        final List<Node> nodes = createSingleItemNodeList(status, "Alpha");

        // Act:
        final NodeStatusDemux demux = new NodeStatusDemux(nodes);

        // Assert:
        Assert.assertThat(demux.getActiveNodes().size(), IsEqual.equalTo(expectedNumActiveNodes));
        Assert.assertThat(demux.getInactiveNodes().size(), IsEqual.equalTo(expectedNumInactiveNodes));
    }

    private static void assertStatusListNodes(
        final NodeStatusDemux demux,
        final String[] expectedActivePlatforms,
        final String[] expectedInactivePlatforms) {
        // Assert:
        Assert.assertThat(getPlatforms(demux.getActiveNodes()), IsEquivalent.equivalentTo(expectedActivePlatforms));
        Assert.assertThat(getPlatforms(demux.getInactiveNodes()), IsEquivalent.equivalentTo(expectedInactivePlatforms));
    }

    //endregion

    //region serialization

    @Test
    public void canRoundTripNodeStatusDemux() {
        // Arrange:
        final List<Node> nodes = new ArrayList<>();
        nodes.add(createNode(NodeStatus.ACTIVE, "A"));
        nodes.add(createNode(NodeStatus.INACTIVE, "B"));
        nodes.add(createNode(NodeStatus.INACTIVE, "C"));
        nodes.add(createNode(NodeStatus.ACTIVE, "D"));
        nodes.add(createNode(NodeStatus.FAILURE, "E"));
        nodes.add(createNode(NodeStatus.ACTIVE, "F"));
        final NodeStatusDemux originalDemux = new NodeStatusDemux(nodes);

        // Assert:
        NodeStatusDemux demux = new NodeStatusDemux(Utils.roundtripSerializableEntity(originalDemux, null));

        // Assert:
        assertStatusListNodes(demux, new String[] { "A", "D", "F" }, new String[] { "B", "C" });
    }

    //endregion

    //region update

    @Test
    public void updateCanAddNewActiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux(new ArrayList<Node>());

        // Act:
        final Node node = createNode(NodeStatus.ACTIVE, "A");
        demux.update(node.getInfo(), NodeStatus.ACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[]{ "A" }, new String[]{ });
    }

    @Test
    public void updateCanAddNewInactiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux(new ArrayList<Node>());

        // Act:
        final Node node = createNode(NodeStatus.INACTIVE, "A");
        demux.update(node.getInfo(), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { }, new String[] { "A" });
    }

    @Test
    public void updateDoesNotAddNewFailureNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux(new ArrayList<Node>());

        // Act:
        final Node node = createNode(NodeStatus.FAILURE, "A");
        demux.update(node.getInfo(), NodeStatus.FAILURE);

        // Assert:
        assertStatusListNodes(demux, new String[] { }, new String[] { });
    }

    @Test
    public void updateCanUpdateActiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux(createSingleItemNodeList(NodeStatus.ACTIVE, "A"));

        // Act:
        final Node node = createNode(NodeStatus.ACTIVE, "B", "A".codePointAt(0));
        demux.update(node.getInfo(), NodeStatus.ACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { "B" }, new String[] { });
    }

    @Test
    public void updateCanUpdateActiveNodeAsInactiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux(createSingleItemNodeList(NodeStatus.ACTIVE, "A"));

        // Act:
        final Node node = createNode(NodeStatus.INACTIVE, "B", "A".codePointAt(0));
        demux.update(node.getInfo(), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { }, new String[] { "B" });
    }

    @Test
    public void updateCanUpdateInactiveNodeAsActiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux(createSingleItemNodeList(NodeStatus.INACTIVE, "A"));

        // Act:
        final Node node = createNode(NodeStatus.ACTIVE, "B", "A".codePointAt(0));
        demux.update(node.getInfo(), NodeStatus.ACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { "B" }, new String[] { });
    }

    @Test
    public void updateCanUpdateInactiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux(createSingleItemNodeList(NodeStatus.INACTIVE, "A"));

        // Act:
        final Node node = createNode(NodeStatus.INACTIVE, "B", "A".codePointAt(0));
        demux.update(node.getInfo(), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { }, new String[] { "B" });
    }

    @Test
    public void updateOnlyUpdatesMatchingNode() {
        // Arrange:
        final List<Node> nodes = new ArrayList<>();
        nodes.add(createNode(NodeStatus.ACTIVE, "A"));
        nodes.add(createNode(NodeStatus.INACTIVE, "B"));
        nodes.add(createNode(NodeStatus.INACTIVE, "C"));
        nodes.add(createNode(NodeStatus.ACTIVE, "D"));
        nodes.add(createNode(NodeStatus.FAILURE, "E"));
        nodes.add(createNode(NodeStatus.ACTIVE, "F"));
        final NodeStatusDemux demux = new NodeStatusDemux(nodes);

        // Act:
        final Node node = createNode(NodeStatus.INACTIVE, "Z", "D".codePointAt(0));
        demux.update(node.getInfo(), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { "A", "F" }, new String[] { "B", "C", "Z" });
    }

    //endregion

    private static List<Node> createSingleItemNodeList(final NodeStatus status, final String platform) {
        // Arrange:
        final List<Node> nodes = new ArrayList<>();
        nodes.add(createNode(status, platform));
        return nodes;
    }

    private static Node createNode(final NodeStatus status, final String platform) {
        // Arrange:
        return createNode(status, platform, platform.codePointAt(0));
    }

    private static Node createNode(final NodeStatus status, final String platform, int port) {
        // Arrange:
        final NodeInfo info = new NodeInfo(new NodeEndpoint("http", "localhost", port), platform, "FooBar");
        final Node node = new Node(info);
        node.setStatus(status);
        return node;
    }
}