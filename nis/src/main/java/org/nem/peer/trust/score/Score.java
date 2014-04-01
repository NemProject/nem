package org.nem.peer.trust.score;

import org.nem.peer.trust.RealDouble;

/**
 * Represents a basic score.
 */
public abstract class Score {

	private final RealDouble score;

	/**
	 * Creates a new score
	 *
	 * @param initialScore The initial score value.
	 */
	public Score(double initialScore) {
		this.score = new RealDouble(initialScore);
	}

	/**
	 * Gets the raw score.
	 *
	 * @return The raw score.
	 */
	public RealDouble score() {
		return this.score;
	}
}
