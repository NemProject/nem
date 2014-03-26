package org.nem.peer.trust;

/**
 * Interface for a trust provider.
 */
public interface TrustProvider {

    /**
     * Calculates a trust score given a NodeExperience.
     *
     * @param experience The node experience.
     * @return The trust score.
     */
    public double calculateTrustScore(final NodeExperience experience);

    /**
     * Calculates a credibility score given two NodeExperiences.
     *
     * @param experience1 One NodeExperience.
     * @param experience2 The other NodeExperience.
     * @return The mutual credibility score.
     */
    public double calculateCredibilityScore(final NodeExperience experience1, final NodeExperience experience2);
}
