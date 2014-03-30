package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.Node;
import org.nem.peer.trust.score.NodeExperience;

public class NodeExperiencePairTest {

    @Test
    public void nodeInfoCanBeCreated() {
        // Arrange:
        final Node node = Utils.createNodeWithPort(81);
        final NodeExperience experience = new NodeExperience();

        // Act:
        final NodeExperiencePair pair = new NodeExperiencePair(node, experience);

        // Assert:
        Assert.assertThat(pair.getNode(), IsSame.sameInstance(node));
        Assert.assertThat(pair.getExperience(), IsSame.sameInstance(experience));
    }
}
