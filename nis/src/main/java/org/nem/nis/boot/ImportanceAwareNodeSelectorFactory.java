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
	private final ReadOnlyAccountStateRepository accountStateRepository;

	/**
	 * Creates a new importance aware node selector factory.
	 *
	 * @param nodeLimit The number of regular nodes that should be communicated with during time synchronization.
	 * @param trustProvider The trust provider.
	 * @param state The network state.
	 * @param poiFacade The poi facade.
	 * @param accountStateRepository The account state repository.
	 */
	public ImportanceAwareNodeSelectorFactory(
			final int nodeLimit,
			final TrustProvider trustProvider,
			final PeerNetworkState state,
			final ReadOnlyPoiFacade poiFacade,
			final ReadOnlyAccountStateRepository accountStateRepository) {
		this.nodeLimit = nodeLimit;
		this.trustProvider = trustProvider;
		this.state = state;
		this.poiFacade = poiFacade;
		this.accountStateRepository = accountStateRepository;
	}

	@Override
	public NodeSelector createNodeSelector() {
		final TrustContext context = this.state.getTrustContext();
		final SecureRandom random = new SecureRandom();
		return new ImportanceAwareNodeSelector(
				this.nodeLimit,
				this.poiFacade,
				this.accountStateRepository,
				new ActiveNodeTrustProvider(this.trustProvider, this.state.getNodes()),
				context,
				random);
	}
}
