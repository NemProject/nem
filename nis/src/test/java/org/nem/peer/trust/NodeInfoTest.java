package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.Node;
import org.nem.peer.trust.score.NodeExperience;

public class NodeInfoTest {

    @Test
    public void nodeInfoCanBeCreated() {
        // Arrange:
        final Node node = Utils.createNodeWithPort(81);
        final NodeExperience experience = new NodeExperience();

        // Act:
        final NodeInfo info = new NodeInfo(node, experience);

        // Assert:
        Assert.assertThat(info.getNode(), IsSame.sameInstance(node));
        Assert.assertThat(info.getExperience(), IsSame.sameInstance(experience));
    }
}
