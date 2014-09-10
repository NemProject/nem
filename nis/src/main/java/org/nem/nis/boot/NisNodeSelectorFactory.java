package org.nem.nis.boot;

import org.nem.peer.*;
import org.nem.peer.trust.*;

import java.security.SecureRandom;

/**
 * NodeSelector factory used by NIS.
 */
public class NisNodeSelectorFactory implements NodeSelectorFactory {
	private final int nodeLimit;
	private final TrustProvider trustProvider;
	private final PeerNetworkState state;

	/**
	 * Creates a new NIS node selector factory.
	 *
	 * @param nodeLimit The number of regular nodes that should be communicated with during broadcasts.
	 * @param trustProvider The trust provider.
	 * @param state The network state.
	 */
	public NisNodeSelectorFactory(
			final int nodeLimit,
			final TrustProvider trustProvider,
			final PeerNetworkState state) {
		this.nodeLimit = nodeLimit;
		this.trustProvider = trustProvider;
		this.state = state;
	}

	@Override
	public NodeSelector createNodeSelector() {
		final TrustContext context = this.state.getTrustContext();
		final SecureRandom random = new SecureRandom();
		return new PreTrustAwareNodeSelector(
				new BasicNodeSelector(
						this.nodeLimit,
						new ActiveNodeTrustProvider(this.trustProvider, this.state.getNodes()),
						context,
						random),
				this.state.getNodes(),
				context,
				random);
	}
}
