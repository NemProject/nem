package org.nem.peer.trust;

/**
 * A trust provider that includes functionality for calculating trust values.
 */
public interface TrustProvider {

	/**
	 * Calculates a trust vector given a trust context.
	 *
	 * @param context The trust context.
	 * @return The trust result.
	 */
	TrustResult computeTrust(final TrustContext context);
}
