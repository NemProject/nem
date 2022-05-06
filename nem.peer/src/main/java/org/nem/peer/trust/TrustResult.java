package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;

/**
 * The result of a trust calculation.
 */
public class TrustResult {
	private final TrustContext context;
	private final ColumnVector trustValues;

	/**
	 * Creates a new result.
	 *
	 * @param context The trust context that was used in the calculation.
	 * @param trustValues The trust values.
	 */
	public TrustResult(final TrustContext context, final ColumnVector trustValues) {
		if (context.getNodes().length != trustValues.size()) {
			throw new IllegalArgumentException("context.getNodes() and trustValues must have same size");
		}

		this.context = context;
		this.trustValues = trustValues;
	}

	/**
	 * Gets the trust context that was used in the calculation.
	 *
	 * @return The trust context.
	 */
	public TrustContext getTrustContext() {
		return this.context;
	}

	/**
	 * Gets the trust values.
	 *
	 * @return The trust values.
	 */
	public ColumnVector getTrustValues() {
		return this.trustValues;
	}
}
