package org.nem.peer.trust;

/**
 * Helper class for collecting experience data for a node
 *
 */
public class NodeExperience {
	private long successfulCalls=0;
	private long failedCalls=0;
	private double localTrustSum=0.0;
	private double localTrust=0.0;
	private double globalTrust=0.0;
	private double feedbackCredibility=0.0;

	public NodeExperience() {
	}
	
	public long getSuccessfulCalls() {
		return successfulCalls;
	}
	
	public void setSuccessfulCalls(long calls) {
		successfulCalls = calls;
	}
	
	public void incSuccessfulCalls() {
		successfulCalls++;
	}

	public long getFailedCalls() {
		return failedCalls;
	}
	
	public void setFailedCalls(long calls) {
		failedCalls = calls;
	}
	
	public void incFailedCalls() {
		failedCalls++;
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
	
	public void setLocalTrust(double localTrust) {
		this.localTrust = localTrust;
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
