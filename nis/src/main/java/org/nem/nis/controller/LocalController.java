package org.nem.nis.controller;

import org.nem.core.model.*;
import org.nem.core.model.ncc.NisRequestResult;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.time.*;
import org.nem.deploy.CommonStarter;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.viewmodels.*;
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

	@Autowired(required = true)
	public LocalController(final RequiredBlockDao blockDao) {
		this.blockDao = blockDao;
	}

	/**
	 * Stops the current NIS server. Afterwards it has to be started via WebStart again.
	 */
	@ClientApi
	@RequestMapping(value = "/shutdown", method = RequestMethod.GET)
	public void shutdown() {
		LOGGER.info(String.format("Async shut-down initiated in %d msec.", SHUTDOWN_DELAY));
		final Runnable r = () -> {
			try {
				Thread.sleep(SHUTDOWN_DELAY);
			} catch (final InterruptedException e) {
				// We do nothing than continuing
			}
			CommonStarter.INSTANCE.stopServer();
		};

		final Thread thread = new Thread(r);
		thread.start();
	}

	@RequestMapping(value = "/heartbeat", method = RequestMethod.GET)
	@PublicApi
	public NisRequestResult heartbeat() {
		return new NisRequestResult(NisRequestResult.TYPE_HEARTBEAT, NisRequestResult.CODE_SUCCESS, "ok");
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
			long timestamp = UnixTime.fromTimeInstant(new TimeInstant(dbBlock.getTimestamp())).getMillis();
			final ExplorerBlockView explorerBlockView = new ExplorerBlockView(
					dbBlock.getHeight(),
					Address.fromPublicKey(dbBlock.getForger().getPublicKey()),
					timestamp,
					dbBlock.getBlockHash(),
					dbBlock.getBlockTransfers().size()
			);
			dbBlock.getBlockTransfers().stream()
					.map(tx -> new ExplorerTransferView(
							tx.getType(),
							Amount.fromMicroNem(tx.getFee()),
							tx.getDeadline(),
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


}
