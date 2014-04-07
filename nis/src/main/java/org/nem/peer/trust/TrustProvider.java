package org.nem.peer.trust;

import org.nem.core.math.Vector;

/**
 * A trust provider that includes functionality for calculating trust values.
 */
public interface TrustProvider {

	/**
	 * Calculates a trust vector given a trust context.
	 *
	 * @param context The trust context.
	 *
	 * @return The trust vector.
	 */
	public Vector computeTrust(final TrustContext context);
}
