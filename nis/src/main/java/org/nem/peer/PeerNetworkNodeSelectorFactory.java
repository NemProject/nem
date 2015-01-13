package org.nem.peer;

import org.nem.deploy.NisConfiguration;
import org.nem.nis.cache.*;
import org.nem.nis.time.synchronization.ImportanceAwareNodeSelector;
import org.nem.peer.trust.*;

import java.security.SecureRandom;

/**
 * A node selector factory used by the peer network.
 */
public class PeerNetworkNodeSelectorFactory {
	private final NisConfiguration nisConfiguration;
	private final TrustProvider trustProvider;
	private final PeerNetworkState state;
	private final ReadOnlyPoiFacade poiFacade;
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a new peer network node selector factory.
	 *
	 * @param nisConfiguration The nis configuration.
	 * @param trustProvider The trust provider.
	 * @param state The network state.
	 * @param poiFacade The poi facade.
	 * @param accountStateCache The account state cache.
	 */
	public PeerNetworkNodeSelectorFactory(
			final NisConfiguration nisConfiguration,
			final TrustProvider trustProvider,
			final PeerNetworkState state,
			final ReadOnlyPoiFacade poiFacade,
			final ReadOnlyAccountStateCache accountStateCache) {
		this.nisConfiguration = nisConfiguration;
		this.trustProvider = trustProvider;
		this.state = state;
		this.poiFacade = poiFacade;
		this.accountStateCache = accountStateCache;
	}

	/**
	 * Creates a node selector for refresh operations.
	 *
	 * @return The node selector.
	 */
	public NodeSelector createRefreshNodeSelector() {
		return this.createDefaultNodeSelector(false);
	}

	/**
	 * Creates a node selector for update operations.
	 *
	 * @return The node selector.
	 */
	public NodeSelector createUpdateNodeSelector() {
		return this.createDefaultNodeSelector(true);
	}

	private NodeSelector createDefaultNodeSelector(final boolean excludeBusyNodes) {
		final TrustContext context = this.state.getTrustContext();
		final SecureRandom random = new SecureRandom();
		return new PreTrustAwareNodeSelector(
				new BasicNodeSelector(
						this.nisConfiguration.getNodeLimit(),
						this.getTrustProvider(excludeBusyNodes),
						context,
						random),
				this.state.getNodes(),
				context,
				random);
	}

	/**
	 * Creates a node selector for time sync operations.
	 *
	 * @return The node selector.
	 */
	public NodeSelector createTimeSyncNodeSelector() {
		final TrustContext context = this.state.getTrustContext();
		final SecureRandom random = new SecureRandom();
		return new ImportanceAwareNodeSelector(
				this.nisConfiguration.getTimeSyncNodeLimit(),
				this.poiFacade,
				this.accountStateCache,
				this.getTrustProvider(true),
				context,
				random);
	}

	private TrustProvider getTrustProvider(final boolean excludeBusyNodes) {
		return excludeBusyNodes
			? new TrustProviderMaskDecorator(this.trustProvider, this.state.getNodes())
			: new TrustProviderMaskDecorator(this.trustProvider, this.state.getNodes(), pc -> !pc.isLocalNode());
	}
}
