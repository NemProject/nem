package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.Node;

import java.util.*;

public class PreTrustedNodesTest {

    @Test
    public void numberOfPreTrustedNodesCanBeReturned() {
        // Arrange:
        final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

        // Assert:
        Assert.assertThat(preTrustedNodes.getNumPreTrustedNodes(), IsEqual.equalTo(3));
    }

    @Test
    public void preTrustedNodesAreIdentifiedCorrectly() {
        // Arrange:
        final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

        // Assert:
        Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(81)), IsEqual.equalTo(true));
        Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(83)), IsEqual.equalTo(true));
        Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(84)), IsEqual.equalTo(true));
    }

    @Test
    public void nonPreTrustedNodesAreIdentifiedCorrectly() {
        // Arrange:
        final PreTrustedNodes preTrustedNodes = createTestPreTrustedNodes();

        // Assert:
        Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(80)), IsEqual.equalTo(false));
        Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(82)), IsEqual.equalTo(false));
        Assert.assertThat(preTrustedNodes.isPreTrusted(Utils.createNodeWithPort(85)), IsEqual.equalTo(false));
    }

    public static PreTrustedNodes createTestPreTrustedNodes() {
        // Arrange:
        Set<Node> nodeSet = new HashSet<>();
        nodeSet.add(Utils.createNodeWithPort(81));
        nodeSet.add(Utils.createNodeWithPort(83));
        nodeSet.add(Utils.createNodeWithPort(84));
        return new PreTrustedNodes(nodeSet);
    }
}
