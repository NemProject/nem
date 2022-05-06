package org.nem.peer.trust.score;

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
	protected Score(final double initialScore) {
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
