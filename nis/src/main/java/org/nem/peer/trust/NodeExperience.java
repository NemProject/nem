package org.nem.peer.trust;

/**
 * Represents experience one node has with another node.
 */
public class NodeExperience {

	private PositiveLong successfulCalls = new PositiveLong(0);
	private PositiveLong failedCalls = new PositiveLong(0);
	private RealDouble localTrustSum = new RealDouble(0);
	private RealDouble localTrust = new RealDouble(0);
	private RealDouble feedbackCredibility = new RealDouble(1.0);

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

    /**
     * Gets the local trust.
     *
     * @return The local trust.
     */
    public RealDouble localTrust() { return this.localTrust; }

    /**
     * Gets the local trust sum.
     * TODO: consider removing this.
     *
     * @return The local trust sum.
     */
    public RealDouble localTrustSum() { return this.localTrustSum; }

    /**
     * Gets the feedback credibility.
     *
     * @return The feedback credibility.
     */
    public RealDouble feedbackCredibility() { return this.feedbackCredibility; }
}
