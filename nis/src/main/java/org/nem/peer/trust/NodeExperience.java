package org.nem.peer.trust;

/**
 * Represents experience one node has with another node.
 */
public class NodeExperience {

	private long successfulCalls;
	private long failedCalls;
	private double localTrustSum;
	private double localTrust;
	private double globalTrust;
	private double feedbackCredibility = 1.0;

    /**
     * Gets the number of successful calls.
     *
     * @return The number of successful calls.
     */
	public long getSuccessfulCalls() {
		return this.successfulCalls;
	}

    /**
     * Sets the number of successful calls.
     *
     * @param calls The number of successful calls.
     */
	public void setSuccessfulCalls(long calls) {
		this.successfulCalls = Math.max(0, calls);
	}

    /**
     * Increments the number of successful calls.
     */
	public void incSuccessfulCalls() {
		++this.successfulCalls;
	}

    /**
     * Gets the number of failed calls.
     *
     * @return The number of failed calls.
     */
	public long getFailedCalls() {
		return this.failedCalls;
	}

    /**
     * Sets the number of failed calls.
     *
     * @param calls The number of failed calls.
     */
	public void setFailedCalls(final long calls) {
        this.failedCalls = Math.max(0, calls);
	}

    /**
     * Increments the number of failed calls.
     */
	public void incFailedCalls() {
		++this.failedCalls;
	}
	
	public double getLocalTrustSum() {
		return localTrustSum;
	}
	
	public void setLocalTrustSum(double trustSum) {
		this.localTrustSum = trustSum;
	}	
	
	public double getLocalTrust() {
		return localTrust;
	}

    /**
     * Sets the local trust.
     *
     * @param localTrust The local trust
     */
	public void setLocalTrust(final double localTrust) {
        this.localTrust = (Double.isNaN(localTrust) || Double.isInfinite(localTrust)) ? 0.0 : localTrust;
	}	
	
	public double getGlobalTrust() {
		return globalTrust;
	}
	
	public void setGlobalTrust(double globalTrust) {
		this.globalTrust = globalTrust;
	}	
	
	public double getFeedbackCredibility() {
		return feedbackCredibility;
	}
	
	public void setFeedbackCredibility(double feedbackCredibility) {
		this.feedbackCredibility = feedbackCredibility;
	}	
}
