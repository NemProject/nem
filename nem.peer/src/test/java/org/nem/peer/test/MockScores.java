package org.nem.peer.test;

import org.nem.peer.trust.score.Scores;

/**
 * A mock Scores implementation.
 */
public class MockScores extends Scores<MockScore> {

	private final double defaultScoreValue;

	/**
	 * Creates a new mock scores collection.
	 */
	public MockScores() {
		this.defaultScoreValue = MockScore.INITIAL_SCORE;
	}

	/**
	 * Creates a new mock scores collection.
	 *
	 * @param defaultScoreValue The default initial value of all mock scores.
	 */
	public MockScores(final double defaultScoreValue) {
		this.defaultScoreValue = defaultScoreValue;
	}

	@Override
	protected MockScore createScore() {
		final MockScore score = new MockScore();
		score.score().set(this.defaultScoreValue);
		return score;
	}
}
