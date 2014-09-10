package org.nem.peer.trust;

import java.util.*;

public class BasicNodeSelectorTest extends NodeSelectorTest {

	/**
	 * Creates the node selector to test.
	 */
	protected NodeSelector createSelector(
			final int maxNodes,
			final TrustProvider trustProvider,
			final TrustContext context,
			final Random random) {
		return new BasicNodeSelector(maxNodes, trustProvider, context, random);
	}
}
