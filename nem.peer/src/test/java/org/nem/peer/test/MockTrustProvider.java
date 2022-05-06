package org.nem.peer.test;

import org.nem.core.math.ColumnVector;
import org.nem.peer.trust.*;

/**
 * A mock TrustProvider implementation.
 */
public class MockTrustProvider implements TrustProvider {
	private final TrustContext trustContext;
	private final ColumnVector trustVector;

	/**
	 * Creates a new mock trust provider.
	 *
	 * @param trustContext The trust context.
	 * @param trustVector The trust vector that should be returned.
	 */
	public MockTrustProvider(final TrustContext trustContext, final ColumnVector trustVector) {
		this.trustContext = trustContext;
		this.trustVector = trustVector;
	}

	@Override
	public TrustResult computeTrust(final TrustContext context) {
		return new TrustResult(this.trustContext, this.trustVector);
	}
}
