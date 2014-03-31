package org.nem.peer.trust.simulation;

/**
 * Behavior of a node when running simulations
 *
 */
public class NodeBehavior {
	/**
	 * Evil node?
	 */
	private boolean evil;
	
	/**
	 * Probability of node returning honest data
	 */
	private double honestDataProbability;
	
	/**
	 * Probability of node returning honest feedback about other nodes
	 */
	private double honestFeedbackProbability;
	
	/**
	 * Excessive downloading?
	 */
	private boolean leech;
	
	/**
	 * Colluding with other nodes?
	 */
	private boolean collusive;
	
	public NodeBehavior(boolean evil, double honestDataProbability, double honestFeedbackProbability, boolean leech, boolean collusive) {
		this.evil = evil;
		this.honestDataProbability = honestDataProbability;
		this.honestFeedbackProbability = honestFeedbackProbability;
		this.leech = leech;
		this.collusive = collusive;
	}
	
	public boolean isEvil() {
		return evil;
	}
	
	public double getHonestDataProbability() {
		return honestDataProbability;
	}
	
	public double getHonestFeedbackProbability() {
		return honestFeedbackProbability;
	}
	
	public boolean isLeech() {
		return leech;
	}
	
	public boolean isCollusive() {
		return collusive;
	}
}
