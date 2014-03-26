package org.nem.peer.trust;

/**
 * Naive trust provider that returns a uniform trust score for all inputs.
 */
public class UniformTrustProvider implements TrustProvider {

    @Override
    public double calculateTrustScore(final NodeExperience experience) {
        return 1;
    }

    @Override
    public double calculateCredibilityScore(final NodeExperience experience1, final NodeExperience experience2) {
        return 0;
    }
}