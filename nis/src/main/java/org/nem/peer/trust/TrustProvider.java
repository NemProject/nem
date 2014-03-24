package org.nem.peer.trust;

/**
 * Interface for a trust provider.
 */
public interface TrustProvider {

    /**
     * Calculates a trust score given a number of successful calls and a number of failed calls.
     *
     * @param numSuccessfulCalls The number of successful calls.
     * @param numFailedCalls The number of failed calls.
     * @return The score.
     */
    public double calculateScore(final long numSuccessfulCalls, final long numFailedCalls);
}
