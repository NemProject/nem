package org.nem.peer.trust.simulation;

/**
 * Behavior of a node when running simulations
 */
public class NodeBehavior {
	/**
	 * Evil node?
	 */
	private final boolean evil;

	/**
	 * Probability of node returning honest data
	 */
	private final double honestDataProbability;

	/**
	 * Probability of node returning honest feedback about other nodes
	 */
	private final double honestFeedbackProbability;

	/**
	 * Excessive downloading?
	 */
	private final boolean leech;

	/**
	 * Colluding with other nodes?
	 */
	private final boolean collusive;

	public NodeBehavior(final boolean evil, final double honestDataProbability, final double honestFeedbackProbability, final boolean leech, final boolean collusive) {
		this.evil = evil;
		this.honestDataProbability = honestDataProbability;
		this.honestFeedbackProbability = honestFeedbackProbability;
		this.leech = leech;
		this.collusive = collusive;
	}

	public boolean isEvil() {
		return this.evil;
	}

	public double getHonestDataProbability() {
		return this.honestDataProbability;
	}

	public double getHonestFeedbackProbability() {
		return this.honestFeedbackProbability;
	}

	@SuppressWarnings("unused")
	public boolean isLeech() {
		return this.leech;
	}

	public boolean isCollusive() {
		return this.collusive;
	}
}
