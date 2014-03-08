package org.nem.peer;

import org.junit.*;
import org.nem.core.test.*;

import java.util.*;

public class NodeCollectionTest {

    //region basic partitioning

     @Test
    public void multipleNodesArePartitionedCorrectly() {
        // Act:
        final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

         // Assert:
         assertStatusListNodes(nodes, new String[]{ "A", "D", "F" }, new String[]{ "B", "C" });
    }

    //endregion

    //region serialization

    @Test
    public void canRoundTripNodeCollection() {
        // Arrange:
        final NodeCollection originalNodes = createNodeCollectionWithMultipleNodes();

        // Assert:
        final NodeCollection nodes = new NodeCollection(Utils.roundtripSerializableEntity(originalNodes, null));

        // Assert:
        assertStatusListNodes(nodes, new String[] { "A", "D", "F" }, new String[] { "B", "C" });
    }

    //endregion

    //region update

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
        assertStatusListNodes(nodes, new String[]{ "A" }, new String[]{ });
    }

    @Test
    public void updateCanAddNewInactiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();

        // Act:
        nodes.update(createNode("A"), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(nodes, new String[] { }, new String[] { "A" });
    }

    @Test
    public void updateDoesNotAddNewFailureNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();

        // Act:
        nodes.update(createNode("A"), NodeStatus.FAILURE);

        // Assert:
        assertStatusListNodes(nodes, new String[] { }, new String[] { });
    }

    @Test
    public void updateCanUpdateActiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(createNode("A"), NodeStatus.ACTIVE);

        // Act:
        nodes.update(createNode("B", "A".codePointAt(0)), NodeStatus.ACTIVE);

        // Assert:
        assertStatusListNodes(nodes, new String[] { "B" }, new String[] { });
    }

    @Test
    public void updateCanUpdateActiveNodeAsInactiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(createNode("A"), NodeStatus.ACTIVE);

        // Act:
        nodes.update(createNode("B", "A".codePointAt(0)), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(nodes, new String[] { }, new String[] { "B" });
    }

    @Test
    public void updateCanUpdateInactiveNodeAsActiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(createNode("A"), NodeStatus.INACTIVE);

        // Act:
        nodes.update(createNode("B", "A".codePointAt(0)), NodeStatus.ACTIVE);

        // Assert:
        assertStatusListNodes(nodes, new String[] { "B" }, new String[] { });
    }

    @Test
    public void updateCanUpdateInactiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(createNode("A"), NodeStatus.INACTIVE);

        // Act:
        nodes.update(createNode("B", "A".codePointAt(0)), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(nodes, new String[] { }, new String[] { "B" });
    }

    @Test
    public void updateOnlyUpdatesMatchingNode() {
        // Arrange:
        final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

        // Act:
        nodes.update(createNode("Z", "D".codePointAt(0)), NodeStatus.INACTIVE);

        // Assert:
        assertStatusListNodes(nodes, new String[] { "A", "F" }, new String[] { "B", "C", "Z" });
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
        final NodeCollection nodes,
        final String[] expectedActivePlatforms,
        final String[] expectedInactivePlatforms) {
        // Assert:
        Assert.assertThat(getPlatforms(nodes.getActiveNodes()), IsEquivalent.equivalentTo(expectedActivePlatforms));
        Assert.assertThat(getPlatforms(nodes.getInactiveNodes()), IsEquivalent.equivalentTo(expectedInactivePlatforms));
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