package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;

/**
 * The result of a trust calculation.
 */
public class TrustResult {
	private final Node[] nodes;
	private final ColumnVector trustValues;

	/**
	 * Creates a new result.
	 *
	 * @param nodes The nodes involved in the calculation.
	 * @param trustValues The trust values.
	 */
	public TrustResult(final Node[] nodes, final ColumnVector trustValues) {
		if (nodes.length != trustValues.size()) {
			throw new IllegalArgumentException("nodes and trustValues must have same size");
		}

		this.nodes = nodes;
		this.trustValues = trustValues;
	}

	/**
	 * Gets the nodes involved in the calculation.
	 *
	 * @return The nodes involved in the calculation.
	 */
	public Node[] getNodes() {
		return this.nodes;
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
