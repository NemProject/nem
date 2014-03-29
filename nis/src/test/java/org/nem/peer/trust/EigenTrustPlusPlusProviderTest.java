package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.Node;
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

    private static class MockEigenTrustPlusPlusProvider extends EigenTrustPlusPlusProvider {

        @Override
        public double calculateCredibilityScore(NodeExperience experience1, NodeExperience experience2) {
            long numSuccessfulCalls = experience1.successfulCalls().get() + experience2.successfulCalls().get();
            return 0 == numSuccessfulCalls ? 100 : numSuccessfulCalls;
        }
    }
}