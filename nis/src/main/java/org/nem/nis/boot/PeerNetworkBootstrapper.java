package org.nem.nis.boot;

import org.nem.nis.NisIllegalStateException;
import org.nem.peer.*;
import org.nem.peer.services.PeerNetworkServicesFactory;
import org.nem.specific.deploy.IpDetectionMode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class for booting a PeerNetwork.
 */
public class PeerNetworkBootstrapper {
	private final IpDetectionMode ipDetectionMode;
	private final PeerNetwork network;
	private final AtomicBoolean canBoot = new AtomicBoolean(true);
	private boolean isBooted;

	/**
	 * Creates a new factory.
	 *
	 * @param state The network state.
	 * @param servicesFactory The network services factory.
	 * @param selectorFactory The node selector factory.
	 * @param ipDetectionMode The desired IP detection mode.
	 */
	public PeerNetworkBootstrapper(final PeerNetworkState state, final PeerNetworkServicesFactory servicesFactory,
			final PeerNetworkNodeSelectorFactory selectorFactory, final IpDetectionMode ipDetectionMode) {
		this.ipDetectionMode = ipDetectionMode;
		this.network = new PeerNetwork(state, servicesFactory, selectorFactory);
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
			throw new NisIllegalStateException(NisIllegalStateException.Reason.NIS_ILLEGAL_STATE_ALREADY_BOOTED);
		}

		final CompletableFuture<Boolean> future = IpDetectionMode.Disabled == this.ipDetectionMode
				? CompletableFuture.completedFuture(true)
				: this.network.boot();

		return future.handle((result, e) -> {
			if (null != e || (!result && IpDetectionMode.AutoRequired == this.ipDetectionMode)) {
				this.canBoot.set(true);
				throw new IllegalStateException("network boot failed", e);
			}

			this.isBooted = true;
			return this.network;
		});
	}
}
