package org.nem.peer.test;

import org.nem.peer.trust.score.Score;

/**
 * A mock Score implementation.
 */
public class MockScore extends Score {

	/**
	 * The initial raw value of a MockScore.
	 */
	public static final double INITIAL_SCORE = 1.4;

	/**
	 * Creates a new mock Score.
	 */
	public MockScore() {
		super(INITIAL_SCORE);
	}
}
