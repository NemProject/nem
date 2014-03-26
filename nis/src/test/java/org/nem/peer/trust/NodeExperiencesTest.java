package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.Node;

import java.security.InvalidParameterException;

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
        Assert.assertThat(experience.localTrust().get(), IsEqual.equalTo(0.0));
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

    @Test
    public void trustMatrixCanBeCalculatedWithDefaultCredibility() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(node1, node2).localTrust().set(7);
        experiences.getNodeExperience(node1, node3).localTrust().set(2);
        experiences.getNodeExperience(node2, node1).localTrust().set(5);
        experiences.getNodeExperience(node2, node3).localTrust().set(4);
        experiences.getNodeExperience(node3, node1).localTrust().set(11);
        experiences.getNodeExperience(node3, node2).localTrust().set(6);

        // Act:
        final Matrix matrix = experiences.getTrustMatrix(new Node[]{ node1, node2, node3 });

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

    @Test
    public void trustMatrixCanBeCalculatedWithCustomCredibility() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(node1, node2).localTrust().set(7);
        experiences.getNodeExperience(node1, node2).feedbackCredibility().set(0.5);
        experiences.getNodeExperience(node2, node1).localTrust().set(5);
        experiences.getNodeExperience(node2, node1).feedbackCredibility().set(0.1);

        // Act:
        final Matrix matrix = experiences.getTrustMatrix(new Node[] { node1, node2 });

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(2));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(2));
        Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(0.5));
        Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(3.5));
        Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
    }

    //endregion

    //region trust vectors

    @Test
    public void trustVectorCanBeRetrieved() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(node1, node2).localTrust().set(7);
        experiences.getNodeExperience(node1, node3).localTrust().set(2);

        // Act:
        final Vector vector = experiences.getLocalTrustVector(node1, new Node[] { node1, node2, node3 });

        // Assert:
        Assert.assertThat(vector.getSize(), IsEqual.equalTo(3));
        Assert.assertThat(vector.getAt(0), IsEqual.equalTo(0.0));
        Assert.assertThat(vector.getAt(1), IsEqual.equalTo(7.0));
        Assert.assertThat(vector.getAt(2), IsEqual.equalTo(2.0));
    }

    @Test(expected = InvalidParameterException.class)
    public void smallerTrustVectorCannotBeSet() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final NodeExperiences experiences = new NodeExperiences();

        final Vector vector = new Vector(2);

        // Act:
        experiences.setLocalTrustVector(node1, new Node[]{ node1, node2, node3 }, vector);
    }

    @Test(expected = InvalidParameterException.class)
    public void largerTrustVectorCannotBeSet() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final NodeExperiences experiences = new NodeExperiences();

        final Vector vector = new Vector(4);

        // Act:
        experiences.setLocalTrustVector(node1, new Node[]{ node1, node2, node3 }, vector);
    }

    @Test
    public void trustVectorCanBeSet() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final Node[] nodes = new Node[] { node1, node2, node3 };
        final NodeExperiences experiences = new NodeExperiences();

        final Vector trustVector = new Vector(3);
        trustVector.setAt(0, 3.0);
        trustVector.setAt(1, 7.0);
        trustVector.setAt(2, 4.0);

        // Act:
        experiences.setLocalTrustVector(node2, nodes, trustVector);
        final Vector vector = experiences.getLocalTrustVector(node2, nodes);

        // Assert:
        Assert.assertThat(vector.getSize(), IsEqual.equalTo(3));
        Assert.assertThat(vector.getAt(0), IsEqual.equalTo(3.0));
        Assert.assertThat(vector.getAt(1), IsEqual.equalTo(7.0));
        Assert.assertThat(vector.getAt(2), IsEqual.equalTo(4.0));
    }

    //endregion

    //region normalizeLocalTrust

    @Test
    public void localTrustValuesCanBeNormalized() {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);
        final Node[] nodes = new Node[] { node1, node2, node3 };
        final NodeExperiences experiences = new NodeExperiences();

        experiences.getNodeExperience(node1, node2).localTrust().set(7);
        experiences.getNodeExperience(node1, node3).localTrust().set(2);
        experiences.getNodeExperience(node2, node1).localTrust().set(5);
        experiences.getNodeExperience(node2, node3).localTrust().set(4);
        experiences.getNodeExperience(node3, node1).localTrust().set(11);
        experiences.getNodeExperience(node3, node2).localTrust().set(6);

        // Act:
        experiences.normalizeLocalTrust(nodes);
        final Matrix matrix = experiences.getTrustMatrix(nodes);

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
        Assert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(5.0 / 9));
        Assert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(11.0 / 17));
        Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(7.0 / 9));
        Assert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(6.0 / 17));
        Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(2.0 / 9));
        Assert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(4.0 / 9));
        Assert.assertThat(matrix.getAt(2, 2), IsEqual.equalTo(0.0));
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
