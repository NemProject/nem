package org.nem.peer.trust;

import java.util.Random;
import org.nem.core.math.ColumnVector;

public class BasicNodeSelectorTest extends NodeSelectorTest {

	/**
	 * Creates the node selector to test.
	 */
	protected NodeSelector createSelector(final int maxNodes, final ColumnVector trustVector, final TrustContext context,
			final Random random) {
		return new BasicNodeSelector(maxNodes, trustVector, context.getNodes(), random);
	}
}
