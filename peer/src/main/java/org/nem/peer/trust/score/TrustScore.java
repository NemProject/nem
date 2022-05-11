package org.nem.peer.trust.score;

/**
 * Represents the trust one node has with another node. The default score is 0.0 (no trust).
 */
public class TrustScore extends Score {

	/**
	 * Creates a new trust score.
	 */
	public TrustScore() {
		super(0.0);
	}
}
