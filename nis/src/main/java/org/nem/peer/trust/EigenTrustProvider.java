package org.nem.peer.trust;

/**
 * Trust provider based on the EigenTrust algorithm.
 */
public class EigenTrustProvider implements TrustProvider {

    @Override
    public double calculateTrustScore(final NodeExperience experience) {
        return Math.max(experience.successfulCalls().get() - experience.failedCalls().get(), 0.0);
    }

    @Override
    public double calculateCredibilityScore(final NodeExperience experience1, final NodeExperience experience2) {
        return 0;
    }
}