package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.Node;
import org.nem.peer.trust.score.NodeExperience;
import org.nem.peer.trust.score.NodeExperiences;

public class NodeExperiencesTest {

    //region basic operations

    @Test
    public void previouslyUnknownNodeExperienceCanBeRetrieved() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final NodeExperiences experiences = new NodeExperiences();

        // Act:
        final NodeExperience experience = experiences.getNodeExperience(node1, node2);

        // Assert:
        Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
    }

    @Test
    public void sameExperienceIsReturnedForSameSourceAndPeerNode() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
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
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final NodeExperiences experiences = new NodeExperiences();

        // Act:
        final NodeExperience experience1 = experiences.getNodeExperience(node1, node2);
        final NodeExperience experience2 = experiences.getNodeExperience(node2, node1);

        // Assert:
        Assert.assertThat(experience2, IsNot.not(IsSame.sameInstance(experience1)));
    }

    //endregion

    //region trust matrix

// TODO: the trust matrix needs to be adjusted!!!
//    @Test
//    public void trustMatrixCanBeCalculatedWithCustomCredibility() {
//        // Arrange:
//        final Node node1 = Utils.createNodeWithPort(81);
//        final Node node2 = Utils.createNodeWithPort(82);
//        final NodeExperiences experiences = new NodeExperiences();
//
//        experiences.getNodeExperience(node1, node2).localTrust().set(7);
//        experiences.getNodeExperience(node1, node2).feedbackCredibility().set(0.5);
//        experiences.getNodeExperience(node2, node1).localTrust().set(5);
//        experiences.getNodeExperience(node2, node1).feedbackCredibility().set(0.1);
//
//        // Act:
//        final Matrix matrix = experiences.getTrustMatrix(new Node[] { node1, node2 });
//
//        // Assert:
//        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(2));
//        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(2));
//        Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
//        Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(0.5));
//        Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(3.5));
//        Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
//    }

    //endregion

    //region shared experiences matrix

    @Test
    public void sharedExperiencesMatrixHasZeroRowForLocalNode() {
        // Act:
        final Matrix matrix = createTotalSharedExperienceMatrix();

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(0.0));
    }

    @Test
    public void sharedExperiencesMatrixHasZeroDiagonal() {
        // Act:
        final Matrix matrix = createTotalSharedExperienceMatrix();

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(2, 2), IsEqual.equalTo(0.0));
    }

    @Test
    public void sharedExperiencesCanHaveOneInOtherCells() {
        // Act:
        final Matrix matrix = createTotalSharedExperienceMatrix();

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.absSum(), IsEqual.equalTo(4.0));
        Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(1.0));
        Assert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(1.0));
        Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(1.0));
        Assert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(1.0));
    }

    private static Matrix createTotalSharedExperienceMatrix() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final Node[] nodes = new Node[] { node1, node2, node3 };
        final NodeExperiences experiences = new NodeExperiences();

        for (final Node nodeI : nodes)
            for (final Node nodeJ : nodes)
                experiences.getNodeExperience(nodeI, nodeJ).successfulCalls().set(1);

        // Act:
        return experiences.getSharedExperienceMatrix(node2, nodes);
    }

    @Test
    public void sharedExperiencesMatrixHasZeroForLocalOnlyInteraction() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final Node[] nodes = new Node[] { node1, node2, node3 };
        final NodeExperiences experiences = new NodeExperiences();

        for (final Node nodeI : nodes)
            experiences.getNodeExperience(node2, nodeI).successfulCalls().set(1);

        // Act:
        final Matrix matrix = experiences.getSharedExperienceMatrix(node2, new Node[]{ node1, node2, node3 });

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.absSum(), IsEqual.equalTo(0.0));
    }

    @Test
    public void sharedExperiencesMatrixHasZeroForExternalOnlyInteraction() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(node1, node3).successfulCalls().set(7);
        experiences.getNodeExperience(node3, node1).failedCalls().set(7);

        // Act:
        final Matrix matrix = experiences.getSharedExperienceMatrix(node2, new Node[]{ node1, node2, node3 });

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.absSum(), IsEqual.equalTo(0.0));
    }

    @Test
    public void sharedExperiencesMatrixHasOneForLocalAndExternalInteraction() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(node1, node3).successfulCalls().set(2);
        experiences.getNodeExperience(node2, node3).failedCalls().set(8);

        // Act:
        final Matrix matrix = experiences.getSharedExperienceMatrix(node2, new Node[]{ node1, node2, node3 });

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.absSum(), IsEqual.equalTo(1.0));
        Assert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(1.0));
    }

    //endregion
}
