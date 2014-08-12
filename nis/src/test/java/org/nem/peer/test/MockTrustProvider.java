package org.nem.peer.test;

import org.nem.core.math.ColumnVector;
import org.nem.peer.trust.*;

/**
 * A mock TrustProvider implementation.
 */
public class MockTrustProvider implements TrustProvider {

	private final ColumnVector trustVector;
	private int numTrustComputations;

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
		++this.numTrustComputations;
		return this.trustVector;
	}

	/**
	 * Gets the number of times computeTrust was called.
	 *
	 * @return The number of times computeTrust was called.
	 */
	public int getNumTrustComputations() {
		return this.numTrustComputations;
	}
}