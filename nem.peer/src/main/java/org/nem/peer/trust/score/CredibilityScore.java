package org.nem.peer.trust.score;

/**
 * Represents the credibility one node has with another node. The default score is 1.0 (full credibility).
 */
public class CredibilityScore extends Score {

	/**
	 * Creates a new credibility score.
	 */
	public CredibilityScore() {
		super(1.0);
	}
}
