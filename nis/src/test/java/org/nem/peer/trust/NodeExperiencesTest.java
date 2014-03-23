package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsSame;
import org.junit.*;
import org.nem.peer.Node;
import org.nem.peer.NodeEndpoint;

public class NodeExperiencesTest {

    @Test
    public void previouslyUnknownNodeExperienceCanBeRetrieved() {
        // Arrange:
        final Node node1 = createNode(81);
        final Node node2 = createNode(82);
        final NodeExperiences experiences = new NodeExperiences();

        // Act:
        final NodeExperience experience = experiences.getNodeExperience(node1, node2);

        // Assert:
        Assert.assertThat(experience.getLocalTrust(), IsEqual.equalTo(0.0));
    }

    @Test
    public void sameExperienceIsReturnedForSameSourceAndPeerNode() {
        // Arrange:
        final Node node1 = createNode(81);
        final Node node2 = createNode(82);
        final NodeExperiences experiences = new NodeExperiences();

        // Act:
        final NodeExperience experience1 = experiences.getNodeExperience(node1, node2);
        final NodeExperience experience2 = experiences.getNodeExperience(node1, node2);

        // Assert:
        Assert.assertThat(experience2, IsSame.sameInstance(experience1));
    }

    @Test
    public void experienceIsDirectional() {
        // Arrange:
        final Node node1 = createNode(81);
        final Node node2 = createNode(82);
        final NodeExperiences experiences = new NodeExperiences();

        // Act:
        final NodeExperience experience1 = experiences.getNodeExperience(node1, node2);
        final NodeExperience experience2 = experiences.getNodeExperience(node2, node1);

        // Assert:
        Assert.assertThat(experience2, IsNot.not(IsSame.sameInstance(experience1)));
    }

    @Test
    public void transposedLocalTrustMatrixCanBeReturned() {
        // Arrange:
        final Node node1 = createNode(81);
        final Node node2 = createNode(82);
        final Node node3 = createNode(83);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(node1, node2).setLocalTrust(7);
        experiences.getNodeExperience(node1, node3).setLocalTrust(2);
        experiences.getNodeExperience(node2, node1).setLocalTrust(5);
        experiences.getNodeExperience(node2, node3).setLocalTrust(4);
        experiences.getNodeExperience(node3, node1).setLocalTrust(11);
        experiences.getNodeExperience(node3, node2).setLocalTrust(6);

        // Act:
        final Matrix matrix = experiences.getTransposedLocalTrustMatrix(new Node[] { node1, node2, node3 });

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(5.0));
        Assert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(11.0));
        Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(7.0));
        Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(6.0));
        Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(2.0));
        Assert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(4.0));
        Assert.assertThat(matrix.getAt(2, 2), IsEqual.equalTo(0.0));
    }

    private static Node createNode(int port) {
        return new Node(new NodeEndpoint("http", "localhost", port), "P", "A");
    }
}
