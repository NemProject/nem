package org.nem.nis.boot;

import org.nem.nis.cache.*;
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
	private final ReadOnlyPoiFacade poiFacade;
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a new importance aware node selector factory.
	 *
	 * @param nodeLimit The number of regular nodes that should be communicated with during time synchronization.
	 * @param trustProvider The trust provider.
	 * @param state The network state.
	 * @param poiFacade The poi facade.
	 * @param accountStateCache The account state cache.
	 */
	public ImportanceAwareNodeSelectorFactory(
			final int nodeLimit,
			final TrustProvider trustProvider,
			final PeerNetworkState state,
			final ReadOnlyPoiFacade poiFacade,
			final ReadOnlyAccountStateCache accountStateCache) {
		this.nodeLimit = nodeLimit;
		this.trustProvider = trustProvider;
		this.state = state;
		this.poiFacade = poiFacade;
		this.accountStateCache = accountStateCache;
	}

	@Override
	public NodeSelector createNodeSelector() {
		final TrustContext context = this.state.getTrustContext();
		final SecureRandom random = new SecureRandom();
		return new ImportanceAwareNodeSelector(
				this.nodeLimit,
				this.poiFacade,
				this.accountStateCache,
				new ActiveNodeTrustProvider(this.trustProvider, this.state.getNodes()),
				context,
				random);
	}
}
