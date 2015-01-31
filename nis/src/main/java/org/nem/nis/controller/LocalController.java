package org.nem.nis.controller;

import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.NemStatus;
import org.nem.core.model.ncc.NemRequestResult;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.ClientApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
public class LocalController {
	private final static Logger LOGGER = Logger.getLogger(LocalController.class.getName());

	private static final long SHUTDOWN_DELAY = 200;
	private final NisPeerNetworkHost host;
	private final CommonStarter starter;

	@Autowired(required = true)
	public LocalController(
			final NisPeerNetworkHost host,
			final CommonStarter starter) {
		this.host = host;
		this.starter = starter;
	}

	/**
	 * Stops the current NIS server. Afterwards it has to be started via WebStart again.
	 * TODO-CR: we should refactor since this is similar (the same as whats in NIS);
	 */
	@ClientApi
	@RequestMapping(value = "/shutdown", method = RequestMethod.GET)
	public void shutdown() {
		LOGGER.info(String.format("Async shut-down initiated in %d msec.", SHUTDOWN_DELAY));
		final Runnable r = () -> {
			ExceptionUtils.propagateVoid(() -> Thread.sleep(SHUTDOWN_DELAY));
			this.starter.stopServer();
		};

		final Thread thread = new Thread(r);
		thread.start();
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
		if (!this.host.isNetworkBooted()) {
			return this.host.isNetworkBooting() ? NemStatus.BOOTING : NemStatus.RUNNING;
		}

		if (this.host.getNetwork().getNodes().getActiveNodes().isEmpty()) {
			return NemStatus.NO_REMOTE_NIS_AVAILABLE;
		}

		return this.host.getNetwork().isChainSynchronized() ? NemStatus.SYNCHRONIZED : NemStatus.BOOTED;
	}
}
