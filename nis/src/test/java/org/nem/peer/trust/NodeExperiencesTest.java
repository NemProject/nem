package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.Node;
import org.nem.peer.trust.score.NodeExperience;
import org.nem.peer.trust.score.NodeExperiences;

import java.util.*;

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

    //region getNodeExperiences / setNodeExperiences

    @Test
    public void getExperiencesReturnsAllNodeExperiences() {
        // Arrange:
        final Node[] nodes = Utils.createNodeArray(4);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(nodes[0], nodes[3]).successfulCalls().set(2);
        experiences.getNodeExperience(nodes[1], nodes[2]).successfulCalls().set(6);
        experiences.getNodeExperience(nodes[0], nodes[1]).successfulCalls().set(7);

        // Act:
        final List<NodeInfo> nodeInfoList = experiences.getNodeExperiences(nodes[0]);

        // Assert:
        Assert.assertThat(nodeInfoList.size(), IsEqual.equalTo(2));
        NodeInfo pair1 = nodeInfoList.get(0);
        NodeInfo pair2 = nodeInfoList.get(1);
        if (pair1.getNode().equals(nodes[3])) {
            final NodeInfo temp = pair1;
            pair1 = pair2;
            pair2 = temp;
        }

        Assert.assertThat(pair1.getNode(), IsEqual.equalTo(nodes[1]));
        Assert.assertThat(pair1.getExperience().successfulCalls().get(), IsEqual.equalTo(7L));

        Assert.assertThat(pair2.getNode(), IsEqual.equalTo(nodes[3]));
        Assert.assertThat(pair2.getExperience().successfulCalls().get(), IsEqual.equalTo(2L));
    }

    @Test
    public void setExperiencesUpdatesAllNodeExperiences() {
        // Arrange:
        final Node[] nodes = Utils.createNodeArray(4);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(nodes[0], nodes[3]).successfulCalls().set(6);

        final List<NodeInfo> nodeInfoList = new ArrayList<>();
        nodeInfoList.add(new NodeInfo(nodes[3], createNodeExperience(2)));
        nodeInfoList.add(new NodeInfo(nodes[1], createNodeExperience(11)));

        // Act:
        experiences.setNodeExperiences(nodes[0], nodeInfoList);

        // Assert:
        Assert.assertThat(
            experiences.getNodeExperience(nodes[0], nodes[1]).successfulCalls().get(),
            IsEqual.equalTo(11L));
        Assert.assertThat(
            experiences.getNodeExperience(nodes[0], nodes[3]).successfulCalls().get(),
            IsEqual.equalTo(2L));
    }

    private static NodeExperience createNodeExperience(final long numSuccessfulCalls) {
        final NodeExperience experience = new NodeExperience();
        experience.successfulCalls().set(numSuccessfulCalls);
        return experience;
    }

    //endregion
}
