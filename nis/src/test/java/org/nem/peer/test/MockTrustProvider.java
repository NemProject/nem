package org.nem.peer.test;

import org.nem.peer.trust.*;

/**
 * A mock TrustProvider implementation.
 */
public class MockTrustProvider implements TrustProvider {

	private final Vector trustVector;

	/**
	 * Creates a new mock trust provider.
	 *
	 * @param trustVector The trust vector that should be returned.
	 */
	public MockTrustProvider(final Vector trustVector) {
		this.trustVector = trustVector;
	}

	@Override
	public Vector computeTrust(final TrustContext context) {
		return this.trustVector;
	}
}