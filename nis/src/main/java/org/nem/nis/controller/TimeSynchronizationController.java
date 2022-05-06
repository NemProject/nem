package org.nem.nis.controller;

import org.nem.core.node.Node;
import org.nem.core.time.TimeProvider;
import org.nem.core.time.synchronization.CommunicationTimeStamps;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.*;
import org.nem.peer.node.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Controller that exposes network time stamps.
 */
@RestController
public class TimeSynchronizationController {
	private final TimeProvider timeProvider;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	public TimeSynchronizationController(final TimeProvider timeProvider, final NisPeerNetworkHost host) {
		this.timeProvider = timeProvider;
		this.host = host;
	}

	/**
	 * Gets the communication time stamps (receive/send network time).
	 *
	 * @return The communication time stamps.
	 */
	@RequestMapping(value = "/time-sync/network-time", method = RequestMethod.GET)
	@ClientApi
	public CommunicationTimeStamps getNetworkTime() {
		return new CommunicationTimeStamps(this.timeProvider.getNetworkTime(), this.timeProvider.getNetworkTime());
	}

	/**
	 * Gets the communication time stamps (receive/send network time).
	 *
	 * @param challenge The challenge.
	 * @return The communication time stamps.
	 */
	@RequestMapping(value = "/time-sync/network-time", method = RequestMethod.POST)
	@P2PApi
	@PublicApi
	@AuthenticatedApi
	public AuthenticatedResponse<CommunicationTimeStamps> getNetworkTime(@RequestBody final NodeChallenge challenge) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		final CommunicationTimeStamps timeStamps = new CommunicationTimeStamps(this.timeProvider.getNetworkTime(),
				this.timeProvider.getNetworkTime());
		return new AuthenticatedResponse<>(timeStamps, localNode.getIdentity(), challenge);
	}
}
