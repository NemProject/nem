package org.nem.nis.time.synchronization;

import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.peer.trust.BasicNodeSelector;

import java.util.Random;

public class ImportanceAwareNodeSelector extends BasicNodeSelector {
	private final ReadOnlyPoxFacade poxFacade;
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a new new importance aware node selector using a custom random number generator.
	 *
	 * @param maxNodes The maximum number of nodes that should be returned from selectNodes.
	 * @param poxFacade The pox facade containing all importance information.
	 * @param accountStateCache The account state cache.
	 * @param trustVector The trust vector.
	 * @param nodes All known nodes.
	 * @param random The random number generator.
	 */
	public ImportanceAwareNodeSelector(final int maxNodes, final ReadOnlyPoxFacade poxFacade,
			final ReadOnlyAccountStateCache accountStateCache, final ColumnVector trustVector, final Node[] nodes, final Random random) {
		super(maxNodes, trustVector, nodes, random);
		this.poxFacade = poxFacade;
		this.accountStateCache = accountStateCache;
	}

	@Override
	protected boolean isCandidate(final Node node) {
		final ReadOnlyAccountState accountState = this.accountStateCache.findLatestForwardedStateByAddress(node.getIdentity().getAddress());
		final ReadOnlyAccountImportance importanceInfo = accountState.getImportanceInfo();
		if (!this.poxFacade.getLastRecalculationHeight().equals(importanceInfo.getHeight())) {
			return false;
		}

		final double importance = importanceInfo.getImportance(this.poxFacade.getLastRecalculationHeight());
		return TimeSynchronizationConstants.REQUIRED_MINIMUM_IMPORTANCE <= importance;
	}
}
