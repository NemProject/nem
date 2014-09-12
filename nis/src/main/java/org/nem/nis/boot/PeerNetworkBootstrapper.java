package org.nem.nis.boot;

import org.nem.peer.*;
import org.nem.peer.services.PeerNetworkServicesFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class for booting a PeerNetwork.
 */
public class PeerNetworkBootstrapper {
	private final boolean requirePeerAck;
	private final PeerNetwork network;
	private final AtomicBoolean canBoot = new AtomicBoolean(true);
	private boolean isBooted;

	/**
	 * Creates a new factory.
	 *
	 * @param state The network state.
	 * @param servicesFactory The network services factory.
	 * @param selectorFactory The node selector factory.
	 * @param requirePeerAck true if the boot should fail if a peer acknowledgement is not received.
	 */
	public PeerNetworkBootstrapper(
			final PeerNetworkState state,
			final PeerNetworkServicesFactory servicesFactory,
			final NodeSelectorFactory selectorFactory,
			final NodeSelectorFactory importanceAwareSelectorFactory,
			final boolean requirePeerAck) {
		this.requirePeerAck = requirePeerAck;
		this.network = new PeerNetwork(state, servicesFactory, selectorFactory, importanceAwareSelectorFactory);
	}

	/**
	 * Gets a value indicating whether or not the network can be booted in its current state.
	 *
	 * @return true if the network can be booted.
	 */
	public boolean canBoot() {
		return this.canBoot.get();
	}

	/**
	 * Gets a value indicating whether or not the network was booted.
	 *
	 * @return true if the network was booted.
	 */
	public boolean isBooted() {
		return this.isBooted;
	}

	/**
	 * Boots the network.
	 *
	 * @return The future.
	 */
	public CompletableFuture<PeerNetwork> boot() {
		if (!this.canBoot.compareAndSet(true, false)) {
			throw new IllegalStateException("network boot was already attempted");
		}

		return this.network.refresh()
				.thenCompose(v -> this.network.updateLocalNodeEndpoint())
				.handle((result, e) -> {
					if (null != e || (!result && this.requirePeerAck)) {
						this.canBoot.set(true);
						throw new IllegalStateException("network boot failed", e);
					}

					this.isBooted = true;
					return this.network;
				});
	}
}
