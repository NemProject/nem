package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.Node;
import org.nem.peer.test.TestTrustContext;
import org.nem.peer.trust.score.*;

public class EigenTrustPlusPlusProviderTest {

    //region feedback credibility vector

    @Test
    public void nodeIsCompletelyCredibleToItself() {
        // Arrange:
        final Node[] nodes = Utils.createNodeArray(3);
        final NodeExperiences experiences = new NodeExperiences();
        final EigenTrustPlusPlusProvider provider = new EigenTrustPlusPlusProvider();

        // Act:
        final Vector vector = provider.calculateFeedbackCredibilityVector(nodes[1], nodes, experiences);

        // Assert:
        Assert.assertThat(vector.getSize(), IsEqual.equalTo(3));
        Assert.assertThat(vector.getAt(1), IsEqual.equalTo(1.0));
    }

    @Test
    public void nodesWithoutSharedPartnersHaveNoCredibility() {
        // Arrange:
        final Node[] nodes = Utils.createNodeArray(3);
        final NodeExperiences experiences = new NodeExperiences();
        final EigenTrustPlusPlusProvider provider = new EigenTrustPlusPlusProvider();

        // Act:
        final Vector vector = provider.calculateFeedbackCredibilityVector(nodes[1], nodes, experiences);

        // Assert:
        Assert.assertThat(vector.getSize(), IsEqual.equalTo(3));
        Assert.assertThat(vector.getAt(0), IsEqual.equalTo(0.0));
        Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.0));
    }

    @Test
    public void nodesWithSharedPartnersHaveCredibilityCorrelatedWithProviderScore() {
        // Arrange:
        final Node[] nodes = Utils.createNodeArray(4);
        final NodeExperiences experiences = new NodeExperiences();
        final EigenTrustPlusPlusProvider provider = new MockEigenTrustPlusPlusProvider();

        experiences.getNodeExperience(nodes[1], nodes[0]).successfulCalls().set(10);
        experiences.getNodeExperience(nodes[3], nodes[0]).successfulCalls().set(2);
        experiences.getNodeExperience(nodes[1], nodes[2]).successfulCalls().set(11);
        experiences.getNodeExperience(nodes[3], nodes[2]).successfulCalls().set(3);

        // Act:
        final Vector vector = provider.calculateFeedbackCredibilityVector(nodes[1], nodes, experiences);

        // Assert:
        Assert.assertThat(vector.getSize(), IsEqual.equalTo(4));
        Assert.assertEquals(21002.73, vector.getAt(3), 0.1);
    }

    //endregion

    //region trust matrix

    @Test
    public void trustMatrixCanBeCalculatedWithCustomCredibility() {
        // Arrange:
        final TestTrustContext testContext = new TestTrustContext();
        final TrustContext context = testContext.getContext();
        final EigenTrustPlusPlusProvider provider = new EigenTrustPlusPlusProvider();
        final Node[] nodes = context.getNodes();

        provider.getTrustScores().getScore(nodes[0], nodes[1]).score().set(7);
        provider.getCredibilityScores().getScore(nodes[0], nodes[1]).score().set(0.5);
        provider.getTrustScores().getScore(nodes[0], nodes[2]).score().set(3.5);
        provider.getCredibilityScores().getScore(nodes[0], nodes[2]).score().set(0.25);
        provider.getTrustScores().getScore(nodes[1], nodes[0]).score().set(5);
        provider.getCredibilityScores().getScore(nodes[1], nodes[0]).score().set(0.1);

        // Act:
        final Matrix matrix = provider.getTrustMatrix(nodes);

        // Assert:
        Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(5));
        Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(5));
        Assert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(0.8));
        Assert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(0.2));
        Assert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(1.0));
    }

    //endregion

    private static class MockEigenTrustPlusPlusProvider extends EigenTrustPlusPlusProvider {

        @Override
        public double calculateCredibilityScore(NodeExperience experience1, NodeExperience experience2) {
            long numSuccessfulCalls = experience1.successfulCalls().get() + experience2.successfulCalls().get();
            return 0 == numSuccessfulCalls ? 100 : numSuccessfulCalls;
        }
    }
}