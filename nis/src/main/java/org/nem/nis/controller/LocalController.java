package org.nem.nis.controller;

import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.*;
import org.nem.core.model.ncc.NemRequestResult;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.time.*;
import org.nem.nis.*;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.dbmodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.logging.Logger;

// TODO: add tests for this controller
@RestController
public class LocalController {
	private final static Logger LOGGER = Logger.getLogger(LocalController.class.getName());

	private final long SHUTDOWN_DELAY = 200;
	private final ReadOnlyBlockDao blockDao;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	public LocalController(final ReadOnlyBlockDao blockDao, final NisPeerNetworkHost host) {
		this.blockDao = blockDao;
		this.host = host;
	}

	/**
	 * Stops the current NIS server. Afterwards it has to be started via WebStart again.
	 * TODO-CR: we should refactor since this is similar (the same as whats in NIS);
	 */
	@ClientApi
	@RequestMapping(value = "/shutdown", method = RequestMethod.GET)
	public void shutdown() {
		LOGGER.info(String.format("Async shut-down initiated in %d msec.", this.SHUTDOWN_DELAY));
		final Runnable r = () -> {
			try {
				Thread.sleep(this.SHUTDOWN_DELAY);
			} catch (final InterruptedException e) {
				// We do nothing than continuing
			}
			CommonStarter.INSTANCE.stopServer();
		};

		final Thread thread = new Thread(r);
		thread.start();
	}

	@RequestMapping(value = "/heartbeat", method = RequestMethod.GET)
	@ClientApi
	public NemRequestResult heartbeat() {
		return new NemRequestResult(NemRequestResult.TYPE_HEARTBEAT, NemRequestResult.CODE_SUCCESS, "ok");
	}

	@RequestMapping(value = "/local/chain/blocks-after", method = RequestMethod.POST)
	@ClientApi
	public SerializableList<ExplorerBlockView> localBlocksAfter(@RequestBody final BlockHeight height) {
		// TODO: add tests for this action
		final SerializableList<ExplorerBlockView> blockList = new SerializableList<>(BlockChainConstants.BLOCKS_LIMIT);
		final Collection<org.nem.nis.dbmodel.Block> dbBlockList = this.blockDao.getBlocksAfter(height, BlockChainConstants.BLOCKS_LIMIT);
		for (final org.nem.nis.dbmodel.Block dbBlock : dbBlockList) {
			final long timeStamp = UnixTime.fromTimeInstant(new TimeInstant(dbBlock.getTimeStamp())).getMillis();
			final ExplorerBlockView explorerBlockView = new ExplorerBlockView(
					dbBlock.getHeight(),
					Address.fromPublicKey(dbBlock.getForger().getPublicKey()),
					timeStamp,
					dbBlock.getBlockHash(),
					dbBlock.getBlockTransfers().size()
			);
			dbBlock.getBlockTransfers().stream()
					.map(tx -> new ExplorerTransferView(
							tx.getType(),
							Amount.fromMicroNem(tx.getFee()),
							tx.getTimeStamp(),
							Address.fromPublicKey(tx.getSender().getPublicKey()),
							tx.getSenderProof(),
							tx.getTransferHash(),
							Address.fromEncoded(tx.getRecipient().getPrintableKey()),
							Amount.fromMicroNem(tx.getAmount()),
							tx.getMessageType(),
							tx.getMessagePayload()
					))
					.forEach(tx -> explorerBlockView.addTransaction(tx));
			blockList.add(explorerBlockView);
		}

		return blockList;
	}

	/**
	 * Gets the NIS status.
	 *
	 * @return The NIS request result.
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

		return this.host.getNetwork().isChainSynchronized() ? NemStatus.SYNCHRONIZED : NemStatus.BOOTED;
	}
}
