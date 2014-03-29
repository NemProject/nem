package org.nem.peer.trust;

import org.nem.peer.trust.score.NodeExperience;

/**
 * Trust provider based on the EigenTrust algorithm.
 */
public class EigenTrustPlusPlusProvider implements TrustProvider {

    @Override
    public double calculateTrustScore(final NodeExperience experience) {
        return experience.successfulCalls().get();
    }

    @Override
    public double calculateCredibilityScore(final NodeExperience experience1, final NodeExperience experience2) {
        return experience1.localTrust().get() * experience1.localTrustSum().get() - experience2.localTrust().get() * experience2.localTrustSum().get();
    }
}