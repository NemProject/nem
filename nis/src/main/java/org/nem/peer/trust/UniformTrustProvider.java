package org.nem.peer.trust;

/**
 * Naive trust provider that returns a uniform trust score for all inputs.
 */
public class UniformTrustProvider implements TrustProvider {

    @Override
    public double calculateScore(final long numSuccessfulCalls, final long numFailedCalls) {
        return 1;
    }
}