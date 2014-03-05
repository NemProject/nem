package org.nem.peer.v2;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class NodeTest {

    private final static NodeEndpoint DEFAULT_ENDPOINT = new NodeEndpoint("ftp", "10.8.8.2", 12);

    @Test
    public void ctorCreatesNewNode() {
        // Arrange:
        NodeInfo info = new NodeInfo(DEFAULT_ENDPOINT, "plat", "app");

        // Act:
        Node node = new Node(info);

        // Assert:
        Assert.assertThat(node.getInfo(), IsEqual.equalTo(info));
        Assert.assertThat(node.getStatus(), IsEqual.equalTo(NodeStatus.INACTIVE));
    }

    @Test
    public void statusCanBeUpdated() {
        // Arrange:
        Node node = new Node(new NodeInfo(DEFAULT_ENDPOINT, "plat", "app"));

        // Act:
        node.setStatus(NodeStatus.ACTIVE);

        // Assert:
        Assert.assertThat(node.getStatus(), IsEqual.equalTo(NodeStatus.ACTIVE));
    }

    @Test
    public void toStringIncludesStatusAndHost() {
        // Act:
        Node node = new Node(new NodeInfo(DEFAULT_ENDPOINT, "plat", "app"));

        // Assert:
        Assert.assertThat(node.toString(), IsEqual.equalTo("Node 10.8.8.2 (INACTIVE)"));
    }
}
