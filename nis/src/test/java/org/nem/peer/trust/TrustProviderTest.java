package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.peer.trust.score.NodeExperience;

public class TrustProviderTest {

    @Test
    public void uniformProviderReturnsSameTrustScoreForAllInputs() {
        // Arrange:
        final TrustProvider provider = new UniformTrustProvider();

        // Assert:
        Assert.assertThat(calculateTrustScore(provider, 1000, 1), IsEqual.equalTo(1.0));
        Assert.assertThat(calculateTrustScore(provider, 1, 1000), IsEqual.equalTo(1.0));
        Assert.assertThat(calculateTrustScore(provider, 1000, 980), IsEqual.equalTo(1.0));
        Assert.assertThat(calculateTrustScore(provider, 21, 1), IsEqual.equalTo(1.0));
    }

    @Test
    public void uniformProviderReturnsSameCredibilityScoreForAllInputs() {
        // Arrange:
        final TrustProvider provider = new UniformTrustProvider();

        // Assert:
        Assert.assertThat(calculateCredibilityScore(provider, 1, 2, 4, 5), IsEqual.equalTo(0.0));
        Assert.assertThat(calculateCredibilityScore(provider, 4, 5, 2, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(calculateCredibilityScore(provider, 1, 1, 1, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(calculateCredibilityScore(provider, 1, 2, 1, 1), IsEqual.equalTo(0.0));
    }

    @Test
    public void eigenTrustProviderProviderReturnsDifferenceOfSuccessfulAndFailureCallsAsTrustScore() {
        // Arrange:
        final TrustProvider provider = new EigenTrustProvider();

        // Assert:
        Assert.assertThat(calculateTrustScore(provider, 1000, 1), IsEqual.equalTo(999.0));
        Assert.assertThat(calculateTrustScore(provider, 1, 1000), IsEqual.equalTo(0.0));
        Assert.assertThat(calculateTrustScore(provider, 1000, 980), IsEqual.equalTo(20.0));
        Assert.assertThat(calculateTrustScore(provider, 21, 1), IsEqual.equalTo(20.0));
    }

    @Test
    public void eigenTrustProviderReturnsSameCredibilityScoreForAllInputs() {
        // Arrange:
        final TrustProvider provider = new EigenTrustProvider();

        // Assert:
        Assert.assertThat(calculateCredibilityScore(provider, 1, 2, 4, 5), IsEqual.equalTo(0.0));
        Assert.assertThat(calculateCredibilityScore(provider, 4, 5, 2, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(calculateCredibilityScore(provider, 1, 1, 1, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(calculateCredibilityScore(provider, 1, 2, 1, 1), IsEqual.equalTo(0.0));
    }

    @Test
    public void eigenTrustPlusPlusProviderProviderReturnsNumberOfSuccessfulCallsAsTrustScore() {
        // Arrange:
        final TrustProvider provider = new EigenTrustPlusPlusProvider();

        // Assert:
        Assert.assertThat(calculateTrustScore(provider, 1000, 1), IsEqual.equalTo(1000.0));
        Assert.assertThat(calculateTrustScore(provider, 1, 1000), IsEqual.equalTo(1.0));
        Assert.assertThat(calculateTrustScore(provider, 1000, 980), IsEqual.equalTo(1000.0));
        Assert.assertThat(calculateTrustScore(provider, 21, 1), IsEqual.equalTo(21.0));
    }

    @Test
    public void eigenTrustProviderReturnsDifferentCredibilityScoreForAllInputs() {
        // Arrange:
        final TrustProvider provider = new EigenTrustPlusPlusProvider();

        // Assert:
        Assert.assertThat(calculateCredibilityScore(provider, 1, 2, 4, 5), IsEqual.equalTo(-18.0));
        Assert.assertThat(calculateCredibilityScore(provider, 4, 5, 2, 1), IsEqual.equalTo(18.0));
        Assert.assertThat(calculateCredibilityScore(provider, 1, 1, 1, 1), IsEqual.equalTo(0.0));
        Assert.assertThat(calculateCredibilityScore(provider, 1, 2, 1, 1), IsEqual.equalTo(1.0));
    }

    private double calculateTrustScore(final TrustProvider provider, final long successfulCalls, final long failedCalls) {
        // Arrange:
        final NodeExperience experience = new NodeExperience();
        experience.successfulCalls().set(successfulCalls);
        experience.failedCalls().set(failedCalls);

        // Act:
        return provider.calculateTrustScore(experience);
    }

    private double calculateCredibilityScore(
        final TrustProvider provider,
        final double localTrust1,
        final double localTrustSum1,
        final double localTrust2,
        final double localTrustSum2) {
        // Arrange:
        final NodeExperience experience1 = new NodeExperience();
        // TODO: these need to be updated
//        experience1.localTrust().set(localTrust1);
//        experience1.localTrustSum().set(localTrustSum1);
//
        final NodeExperience experience2 = new NodeExperience();
//        experience2.localTrust().set(localTrust2);
//        experience2.localTrustSum().set(localTrustSum2);

        // Act:
        return provider.calculateCredibilityScore(experience1, experience2);
    }
}
