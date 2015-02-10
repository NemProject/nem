package org.nem.nis.time.synchronization;

import org.nem.core.node.Node;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.peer.trust.*;

import java.util.Random;

public class ImportanceAwareNodeSelector extends BasicNodeSelector {
	private final ReadOnlyPoiFacade poiFacade;
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a new new importance aware node selector using a custom random number generator.
	 *
	 * @param maxNodes The maximum number of nodes that should be returned from selectNodes.
	 * @param poiFacade The POI facade containing all importance information.
	 * @param accountStateCache The account state cache.
	 * @param trustProvider The trust provider.
	 * @param context The trust context.
	 * @param random The random number generator.
	 */
	public ImportanceAwareNodeSelector(
			final int maxNodes,
			final ReadOnlyPoiFacade poiFacade,
			final ReadOnlyAccountStateCache accountStateCache,
			final TrustProvider trustProvider,
			final TrustContext context,
			final Random random) {
		super(maxNodes, trustProvider, context, random);
		this.poiFacade = poiFacade;
		this.accountStateCache = accountStateCache;
	}

	@Override
	protected boolean isCandidate(final Node node) {
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(node.getIdentity().getAddress());
		final ReadOnlyAccountImportance importanceInfo = accountState.getImportanceInfo();
		if (!this.poiFacade.getLastPoiRecalculationHeight().equals(importanceInfo.getHeight())) {
			return false;
		}

		final double importance = importanceInfo.getImportance(this.poiFacade.getLastPoiRecalculationHeight());
		return TimeSynchronizationConstants.REQUIRED_MINIMUM_IMPORTANCE <= importance;
	}
}
