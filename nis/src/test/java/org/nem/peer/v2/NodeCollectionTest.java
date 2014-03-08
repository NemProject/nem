package org.nem.peer.v2;

import org.junit.*;
import org.nem.core.test.*;

import java.util.*;

public class NodeStatusDemuxTest {

    //region basic partitioning

     @Test
    public void multipleNodesArePartitionedCorrectly() {
        // Act:
        final NodeStatusDemux demux = createNodeCollectionWithMultipleNodes();

         // Assert:
         assertStatusListNodes(demux, new String[]{ "A", "D", "F" }, new String[]{ "B", "C" });
    }

    //endregion

    //region serialization

    @Test
    public void canRoundTripNodeStatusDemux() {
        // Arrange:
        final NodeStatusDemux originalDemux = createNodeCollectionWithMultipleNodes();

        // Assert:
        final NodeStatusDemux demux = new NodeStatusDemux(Utils.roundtripSerializableEntity(originalDemux, null));

        // Assert:
        assertStatusListNodes(demux, new String[] { "A", "D", "F" }, new String[] { "B", "C" });
    }

    //endregion

    //region update

    @Test(expected = NullPointerException.class)
    public void updateCannotAddNullNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux();

        // Act:
        demux.update(null, NodeStatus.ACTIVE);
    }

    @Test
    public void updateCanAddNewActiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux();

        // Act:
        demux.update(createNode("A"), NodeStatus.ACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[]{ "A" }, new String[]{ });
    }

    @Test
    public void updateCanAddNewInactiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux();

        // Act:
        demux.update(createNode("A"), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { }, new String[] { "A" });
    }

    @Test
    public void updateDoesNotAddNewFailureNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux();

        // Act:
        demux.update(createNode("A"), NodeStatus.FAILURE);

        // Assert:
        assertStatusListNodes(demux, new String[] { }, new String[] { });
    }

    @Test
    public void updateCanUpdateActiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux();
        demux.update(createNode("A"), NodeStatus.ACTIVE);

        // Act:
        demux.update(createNode("B", "A".codePointAt(0)), NodeStatus.ACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { "B" }, new String[] { });
    }

    @Test
    public void updateCanUpdateActiveNodeAsInactiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux();
        demux.update(createNode("A"), NodeStatus.ACTIVE);

        // Act:
        demux.update(createNode("B", "A".codePointAt(0)), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { }, new String[] { "B" });
    }

    @Test
    public void updateCanUpdateInactiveNodeAsActiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux();
        demux.update(createNode("A"), NodeStatus.INACTIVE);

        // Act:
        demux.update(createNode("B", "A".codePointAt(0)), NodeStatus.ACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { "B" }, new String[] { });
    }

    @Test
    public void updateCanUpdateInactiveNode() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux();
        demux.update(createNode("A"), NodeStatus.INACTIVE);

        // Act:
        demux.update(createNode("B", "A".codePointAt(0)), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { }, new String[] { "B" });
    }

    @Test
    public void updateOnlyUpdatesMatchingNode() {
        // Arrange:
        final NodeStatusDemux demux = createNodeCollectionWithMultipleNodes();

        // Act:
        demux.update(createNode("Z", "D".codePointAt(0)), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(demux, new String[] { "A", "F" }, new String[] { "B", "C", "Z" });
    }

    //endregion

    private static Node createNode(final String platform) {
        // Arrange:
        return createNode(platform, platform.codePointAt(0));
    }

    private static Node createNode(final String platform, int port) {
        // Arrange:
        return new Node(new NodeEndpoint("http", "localhost", port), platform, "FooBar");
    }

    private static List<String> getPlatforms(final Collection<Node> nodes) {
        final List<String> platforms = new ArrayList<>();
        for (final Node node : nodes)
            platforms.add(node.getPlatform());
        return platforms;
    }

    private static void assertStatusListNodes(
        final NodeStatusDemux demux,
        final String[] expectedActivePlatforms,
        final String[] expectedInactivePlatforms) {
        // Assert:
        Assert.assertThat(getPlatforms(demux.getActiveNodes()), IsEquivalent.equivalentTo(expectedActivePlatforms));
        Assert.assertThat(getPlatforms(demux.getInactiveNodes()), IsEquivalent.equivalentTo(expectedInactivePlatforms));
    }

    private static NodeStatusDemux createNodeCollectionWithMultipleNodes() {
        // Arrange:
        final NodeStatusDemux demux = new NodeStatusDemux();
        demux.update(createNode("A"), NodeStatus.ACTIVE);
        demux.update(createNode("B"), NodeStatus.INACTIVE);
        demux.update(createNode("C"), NodeStatus.INACTIVE);
        demux.update(createNode("D"), NodeStatus.ACTIVE);
        demux.update(createNode("E"), NodeStatus.FAILURE);
        demux.update(createNode("F"), NodeStatus.ACTIVE);
        return demux;
    }
}