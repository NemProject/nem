package org.nem.peer.node;

import java.security.SecureRandom;

/**
 * Factory for creating NodeChallenge.
 */
public class NodeChallengeFactory {

	private static final int CHALLENGE_SIZE = 64;
	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * Gets the next node challenge.
	 *
	 * @return A node challenge.
	 */
	public NodeChallenge next() {
		final byte[] bytes = new byte[CHALLENGE_SIZE];
		this.secureRandom.nextBytes(bytes);
		return new NodeChallenge(bytes);
	}
}
