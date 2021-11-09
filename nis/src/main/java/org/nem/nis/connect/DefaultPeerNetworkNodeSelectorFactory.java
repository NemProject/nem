package org.nem.nis.connect;

import org.nem.nis.cache.*;
import org.nem.nis.time.synchronization.ImportanceAwareNodeSelector;
import org.nem.peer.*;
import org.nem.peer.trust.*;
import org.nem.specific.deploy.NisConfiguration;

import java.security.SecureRandom;

/**
 * A node selector factory used by the peer network.
 */
public class DefaultPeerNetworkNodeSelectorFactory implements PeerNetworkNodeSelectorFactory {
	private final NisConfiguration nisConfiguration;
	private final TrustProvider trustProvider;
	private final PeerNetworkState state;
	private final ReadOnlyPoxFacade poxFacade;
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a new peer network node selector factory.
	 *
	 * @param nisConfiguration The nis configuration.
	 * @param trustProvider The trust provider.
	 * @param state The network state.
	 * @param poxFacade The pox facade.
	 * @param accountStateCache The account state cache.
	 */
	public DefaultPeerNetworkNodeSelectorFactory(final NisConfiguration nisConfiguration, final TrustProvider trustProvider,
			final PeerNetworkState state, final ReadOnlyPoxFacade poxFacade, final ReadOnlyAccountStateCache accountStateCache) {
		this.nisConfiguration = nisConfiguration;
		this.trustProvider = trustProvider;
		this.state = state;
		this.poxFacade = poxFacade;
		this.accountStateCache = accountStateCache;
	}

	@Override
	public NodeSelector createRefreshNodeSelector() {
		return this.createDefaultNodeSelector(false);
	}

	@Override
	public NodeSelector createUpdateNodeSelector() {
		return this.createDefaultNodeSelector(true);
	}

	private NodeSelector createDefaultNodeSelector(final boolean excludeBusyNodes) {
		final TrustContext context = this.state.getTrustContext();
		final SecureRandom random = new SecureRandom();
		final TrustResult trustResult = this.getTrustProvider(excludeBusyNodes).computeTrust(context);
		return new PreTrustAwareNodeSelector(new BasicNodeSelector(this.nisConfiguration.getNodeLimit(), trustResult.getTrustValues(),
				trustResult.getTrustContext().getNodes(), random), this.state.getNodes(), context, random);
	}

	@Override
	public NodeSelector createTimeSyncNodeSelector() {
		final TrustContext context = this.state.getTrustContext();
		final SecureRandom random = new SecureRandom();
		final TrustResult trustResult = this.getTrustProvider(true).computeTrust(context);
		return new ImportanceAwareNodeSelector(this.nisConfiguration.getTimeSyncNodeLimit(), this.poxFacade, this.accountStateCache,
				trustResult.getTrustValues(), trustResult.getTrustContext().getNodes(), random);
	}

	private TrustProvider getTrustProvider(final boolean excludeBusyNodes) {
		return excludeBusyNodes
				? new TrustProviderMaskDecorator(this.trustProvider, this.state.getNodes())
				: new TrustProviderMaskDecorator(this.trustProvider, this.state.getNodes(), pc -> !pc.isLocalNode());
	}
}
