package org.nem.nis.time.synchronization;

import org.nem.core.node.Node;
import org.nem.nis.poi.*;
import org.nem.peer.trust.*;

import java.security.SecureRandom;
import java.util.Random;

// TODO 20140909 i would actually have this derive from basic node selector by adding a virtual function to basicnodeselector like isCandidate(Node)
// TODO 20140910 BR -> J good point, done.

public class ImportanceAwareNodeSelector extends BasicNodeSelector {
	private final PoiFacade poiFacade;

	/**
	 * Creates a new importance aware node selector.
	 *
	 * @param maxNodes The maximum number of nodes that should be returned from selectNodes.
	 * @param poiFacade The POI facade containing all importance information.
	 * @param trustProvider The trust provider.
	 * @param context The trust context.
	 */
	public ImportanceAwareNodeSelector(
			final int maxNodes,
			final PoiFacade poiFacade,
			final TrustProvider trustProvider,
			final TrustContext context) {
		this(maxNodes, poiFacade, trustProvider, context, new SecureRandom());
	}

	/**
	 * Creates a new new importance aware node selector using a custom random number generator.
	 *
	 * @param maxNodes The maximum number of nodes that should be returned from selectNodes.
	 * @param poiFacade The POI facade containing all importance information.
	 * @param trustProvider The trust provider.
	 * @param context The trust context.
	 * @param random The random number generator.
	 */
	public ImportanceAwareNodeSelector(
			final int maxNodes,
			final PoiFacade poiFacade,
			final TrustProvider trustProvider,
			final TrustContext context,
			final Random random) {
		super(maxNodes, trustProvider, context, random);
		this.poiFacade = poiFacade;
	}

	protected boolean isCandidate(final Node node) {
		final PoiAccountState accountState = this.poiFacade.findStateByAddress(node.getIdentity().getAddress());
		if (!this.poiFacade.getLastPoiRecalculationHeight().equals(accountState.getImportanceInfo().getHeight())) {
			return false;
		}
		final double importance = accountState.getImportanceInfo().getImportance(this.poiFacade.getLastPoiRecalculationHeight());
		return TimeSynchronizationConstants.REQUIRED_MINIMUM_IMPORTANCE <= importance;
	}
}
