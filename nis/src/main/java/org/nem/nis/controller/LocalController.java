package org.nem.nis.controller;

import org.nem.core.model.NemStatus;
import org.nem.core.model.ncc.NemRequestResult;
import org.nem.deploy.CommonStarter;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class LocalController {
	private final NisPeerNetworkHost host;
	private final CommonStarter starter;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;

	@Autowired(required = true)
	public LocalController(final NisPeerNetworkHost host, final CommonStarter starter,
			final BlockChainLastBlockLayer blockChainLastBlockLayer) {
		this.host = host;
		this.starter = starter;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
	}

	/**
	 * Stops the current NIS server. Afterwards it has to be started via WebStart again.
	 */
	@RequestMapping(value = "/shutdown", method = RequestMethod.GET)
	@ClientApi
	@TrustedApi
	public void shutdown() {
		this.starter.stopServerAsync();
	}

	/**
	 * Gets a heartbeat from the running NIS node.
	 *
	 * @return The heartbeat response.
	 */
	@RequestMapping(value = "/heartbeat", method = RequestMethod.GET)
	@ClientApi
	public NemRequestResult heartbeat() {
		return new NemRequestResult(NemRequestResult.TYPE_HEARTBEAT, NemRequestResult.CODE_SUCCESS, "ok");
	}

	/**
	 * Gets status from the running NIS node.
	 *
	 * @return The status.
	 */
	@RequestMapping(value = "/status", method = RequestMethod.GET)
	@ClientApi
	public NemRequestResult status() {
		return new NemRequestResult(NemRequestResult.TYPE_STATUS, this.getStatusFromHost().getValue(), "status");
	}

	private NemStatus getStatusFromHost() {
		if (this.blockChainLastBlockLayer.isLoading()) {
			return NemStatus.LOADING;
		}

		if (!this.host.isNetworkBooted()) {
			return this.host.isNetworkBooting() ? NemStatus.BOOTING : NemStatus.RUNNING;
		}

		if (this.host.getNetwork().getNodes().getActiveNodes().isEmpty()) {
			return NemStatus.NO_REMOTE_NIS_AVAILABLE;
		}

		return this.host.getNetwork().isChainSynchronized() ? NemStatus.SYNCHRONIZED : NemStatus.BOOTED;
	}
}
