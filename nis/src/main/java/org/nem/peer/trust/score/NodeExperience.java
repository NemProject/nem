package org.nem.peer.trust.score;

import org.nem.peer.trust.PositiveLong;

/**
 * Represents experience one node has with another node.
 */
public class NodeExperience {

	private PositiveLong successfulCalls = new PositiveLong(0);
	private PositiveLong failedCalls = new PositiveLong(0);

    /**
     * Gets the number of successful calls.
     *
     * @return The number of successful calls.
     */
	public PositiveLong successfulCalls() { return this.successfulCalls; }

    /**
     * Gets the number of failed calls.
     *
     * @return The number of failed calls.
     */
    public PositiveLong failedCalls() { return this.failedCalls; }

    /**
     * Gets the total number of calls.
     *
     * @return The total number of calls.
     */
    public long totalCalls() { return this.successfulCalls.get() + this.failedCalls().get(); }
}
