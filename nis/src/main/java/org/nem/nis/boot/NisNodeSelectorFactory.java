package org.nem.nis.boot;

import org.nem.peer.*;
import org.nem.peer.trust.*;

import java.security.SecureRandom;

/**
 * NodeSelector factor used by NIS.
 */
public class NisNodeSelectorFactory implements NodeSelectorFactory {
	private final Config config;
	private final PeerNetworkState state;

	public NisNodeSelectorFactory(final Config config, final PeerNetworkState state) {
		this.config = config;
		this.state = state;
	}

	@Override
	public NodeSelector createNodeSelector() {
		final TrustContext context = this.state.getTrustContext();
		final SecureRandom random = new SecureRandom();
		return new PreTrustAwareNodeSelector(
				new BasicNodeSelector(
						10, // TODO: read from configuration
						new ActiveNodeTrustProvider(this.config.getTrustProvider(), this.state.getNodes()),
						context,
						random),
				this.state.getNodes(),
				context,
				random);
	}
}
