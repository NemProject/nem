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
        final Node[] nodes = Utils.createNodeArray(2);
        final NodeExperiences experiences = new NodeExperiences();

        // Act:
        final NodeExperience experience = experiences.getNodeExperience(nodes[0], nodes[1]);

        // Assert:
        Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
    }

    @Test
    public void sameExperienceIsReturnedForSameSourceAndPeerNode() {
        // Arrange:
        final Node[] nodes = Utils.createNodeArray(2);
        final NodeExperiences experiences = new NodeExperiences();

        // Act:
        final NodeExperience experience1 = experiences.getNodeExperience(nodes[0], nodes[1]);
        final NodeExperience experience2 = experiences.getNodeExperience(nodes[0], nodes[1]);

        // Assert:
        Assert.assertThat(experience2, IsSame.sameInstance(experience1));
    }

    @Test
    public void experienceIsDirectional() {
        // Arrange:
        final Node[] nodes = Utils.createNodeArray(2);
        final NodeExperiences experiences = new NodeExperiences();

        // Act:
        final NodeExperience experience1 = experiences.getNodeExperience(nodes[0], nodes[1]);
        final NodeExperience experience2 = experiences.getNodeExperience(nodes[1], nodes[0]);

        // Assert:
        Assert.assertThat(experience2, IsNot.not(IsSame.sameInstance(experience1)));
    }

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
        final Node[] nodes = Utils.createNodeArray(3);
        final NodeExperiences experiences = new NodeExperiences();

        for (final Node nodeI : nodes)
            for (final Node nodeJ : nodes)
                experiences.getNodeExperience(nodeI, nodeJ).successfulCalls().set(1);

        // Act:
        return experiences.getSharedExperienceMatrix(nodes[1], nodes);
    }

    @Test
    public void sharedExperiencesMatrixHasZeroForLocalOnlyInteraction() {
        // Arrange:
        final Node[] nodes = Utils.createNodeArray(3);
        final NodeExperiences experiences = new NodeExperiences();

        for (final Node nodeI : nodes)
            experiences.getNodeExperience(nodes[1], nodeI).successfulCalls().set(1);

        // Act:
        final Matrix matrix = experiences.getSharedExperienceMatrix(nodes[1], nodes);

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.absSum(), IsEqual.equalTo(0.0));
    }

    @Test
    public void sharedExperiencesMatrixHasZeroForExternalOnlyInteraction() {
        // Arrange:
        final Node[] nodes = Utils.createNodeArray(3);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(nodes[0], nodes[2]).successfulCalls().set(7);
        experiences.getNodeExperience(nodes[2], nodes[0]).failedCalls().set(7);

        // Act:
        final Matrix matrix = experiences.getSharedExperienceMatrix(nodes[1], nodes);

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.absSum(), IsEqual.equalTo(0.0));
    }

    @Test
    public void sharedExperiencesMatrixHasOneForLocalAndExternalInteraction() {
        // Arrange:
        final Node[] nodes = Utils.createNodeArray(3);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(nodes[0], nodes[2]).successfulCalls().set(2);
        experiences.getNodeExperience(nodes[1], nodes[2]).failedCalls().set(8);

        // Act:
        final Matrix matrix = experiences.getSharedExperienceMatrix(nodes[1], nodes);

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.absSum(), IsEqual.equalTo(1.0));
        Assert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(1.0));
    }

    //endregion
}
