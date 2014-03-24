package org.nem.peer.trust;

/**
 * Trust provider based on the EigenTrust algorithm.
 */
public class EigenTrustProvider implements TrustProvider {

    @Override
    public double calculateScore(final long numSuccessfulCalls, final long numFailedCalls) {
        return Math.max(numSuccessfulCalls - numFailedCalls, 0.0);
    }
}