package org.nem.peer.test;

import org.nem.core.math.ColumnVector;
import org.nem.peer.trust.*;

/**
 * A mock TrustProvider implementation.
 */
public class MockTrustProvider implements TrustProvider {

	private final ColumnVector trustVector;

	/**
	 * Creates a new mock trust provider.
	 *
	 * @param trustVector The trust vector that should be returned.
	 */
	public MockTrustProvider(final ColumnVector trustVector) {
		this.trustVector = trustVector;
	}

	@Override
	public ColumnVector computeTrust(final TrustContext context) {
		return this.trustVector;
	}
}