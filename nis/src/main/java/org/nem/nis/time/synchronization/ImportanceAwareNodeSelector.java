package org.nem.nis.time.synchronization;

import org.nem.core.node.Node;
import org.nem.nis.poi.*;
import org.nem.peer.trust.*;

import java.util.Random;

public class ImportanceAwareNodeSelector extends BasicNodeSelector {
	private final PoiFacade poiFacade;

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
