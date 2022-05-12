package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;

import java.util.Random;

public class BasicNodeSelectorTest extends NodeSelectorTest {

	/**
	 * Creates the node selector to test.
	 */
	protected NodeSelector createSelector(final int maxNodes, final ColumnVector trustVector, final TrustContext context,
			final Random random) {
		return new BasicNodeSelector(maxNodes, trustVector, context.getNodes(), random);
	}
}
