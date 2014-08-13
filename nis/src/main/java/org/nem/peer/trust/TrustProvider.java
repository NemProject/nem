package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;

/**
 * A trust provider that includes functionality for calculating trust values.
 */
public interface TrustProvider {

	/**
	 * Calculates a trust vector given a trust context.
	 *
	 * @param context The trust context.
	 * @return The trust vector.
	 */
	public ColumnVector computeTrust(final TrustContext context);
}
