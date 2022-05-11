package org.nem.peer.trust.score;

/**
 * A collection of CredibilityScore objects.
 */
public class CredibilityScores extends Scores<CredibilityScore> {

	@Override
	protected CredibilityScore createScore() {
		return new CredibilityScore();
	}
}
