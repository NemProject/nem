package org.nem.nis.controller;

import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.*;
import org.nem.core.model.ncc.NemRequestResult;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.time.*;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.secret.BlockChainConstants;
import org.nem.nis.service.RequiredBlockDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

// TODO: add tests for this controller
@RestController
public class LocalController {
	private final static Logger LOGGER = Logger.getLogger(LocalController.class.getName());

	private final long SHUTDOWN_DELAY = 200;
	private final RequiredBlockDao blockDao;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	public LocalController(final RequiredBlockDao blockDao, final NisPeerNetworkHost host) {
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
		org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(height);
		final SerializableList<ExplorerBlockView> blockList = new SerializableList<>(BlockChainConstants.BLOCKS_LIMIT);
		for (int i = 0; i < BlockChainConstants.BLOCKS_LIMIT; ++i) {
			final Long curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				break;
			}

			dbBlock = this.blockDao.findById(curBlockId);
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
	 * TODO 20140909: J-B please add tests for this (they should be simple)
	 * TODO 20140910: BR -> J done. (tests for the above requests still missing though)
	 *
	 * @return The NIS request result.
	 */
	@RequestMapping(value = "/status", method = RequestMethod.GET)
	@ClientApi
	public NemRequestResult status() {
		if (!this.host.isNetworkBooted()) {
			return new NemRequestResult(NemRequestResult.TYPE_STATUS,  NemStatus.RUNNING.getValue(), "status");
		}

		int status = this.host.getNetwork().isChainSynchronized()? NemStatus.SYNCHRONIZED.getValue() : NemStatus.BOOTED.getValue();
		return new NemRequestResult(NemRequestResult.TYPE_STATUS, status, "status");
	}
}
