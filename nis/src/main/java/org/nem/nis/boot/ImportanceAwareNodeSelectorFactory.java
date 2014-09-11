package org.nem.nis.boot;

import org.nem.nis.poi.PoiFacade;
import org.nem.nis.time.synchronization.ImportanceAwareNodeSelector;
import org.nem.peer.*;
import org.nem.peer.trust.*;

import java.security.SecureRandom;

/**
 * Importance aware node selector factory.
 */
public class ImportanceAwareNodeSelectorFactory implements NodeSelectorFactory {
	private final int nodeLimit;
	private final TrustProvider trustProvider;
	private final PeerNetworkState state;
	private final PoiFacade poiFacade;

	/**
	 * Creates a new importance aware node selector factory.
	 *
	 * @param nodeLimit The number of regular nodes that should be communicated with during time synchronization.
	 * @param trustProvider The trust provider.
	 * @param state The network state.
	 * @param poiFacade The Poi facade.
	 */
	public ImportanceAwareNodeSelectorFactory(
			final int nodeLimit,
			final TrustProvider trustProvider,
			final PeerNetworkState state,
			final PoiFacade poiFacade) {
		this.nodeLimit = nodeLimit;
		this.trustProvider = trustProvider;
		this.state = state;
		this.poiFacade = poiFacade;
	}

	@Override
	public NodeSelector createNodeSelector() {
		final TrustContext context = this.state.getTrustContext();
		final SecureRandom random = new SecureRandom();
		return new ImportanceAwareNodeSelector(
				this.nodeLimit,
				this.poiFacade,
				new ActiveNodeTrustProvider(this.trustProvider, this.state.getNodes()),
				context,
				random);
	}
}
