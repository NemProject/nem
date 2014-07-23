package org.nem.nis.controller;

import java.util.logging.Logger;

import org.nem.core.model.Address;
import org.nem.core.model.BlockChainConstants;
import org.nem.core.model.ncc.NisRequestResult;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.serialization.SerializableList;
import org.nem.core.time.SystemTimeProvider;
import org.nem.deploy.CommonStarter;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.annotations.PublicApi;
import org.nem.nis.controller.viewmodels.ExplorerBlockView;
import org.nem.nis.controller.viewmodels.ExplorerTransferView;
import org.nem.nis.service.RequiredBlockDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocalController {
	private final static Logger LOGGER = Logger.getLogger(LocalController.class.getName());

	private final long SHUTDOWN_DELAY = 200;
	private final RequiredBlockDao blockDao;
	private final AccountLookup accountLookup;


	@Autowired(required = true)
	public LocalController(
			final RequiredBlockDao blockDao,
			final AccountLookup accountLookup) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
	}

	/**
	 * Stops the current NIS server. Afterwards it has to be started via WebStart again.
	 */
	@RequestMapping(value = "/shutdown", method = RequestMethod.GET)
	public void shutdown() {
		LOGGER.info(String.format("Async shut-down initiated in %d msec.", SHUTDOWN_DELAY));
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(SHUTDOWN_DELAY);
				} catch (final InterruptedException e) {
					// We do nothing than continuing
				}
				CommonStarter.INSTANCE.stopServer();
			}
		};

		final Thread thread = new Thread(r);
		thread.start();
	}

	@RequestMapping(value = "/heartbeat", method = RequestMethod.GET)
	@PublicApi
	public NisRequestResult heartbeat()
	{
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
			long timestamp = SystemTimeProvider.getEpochTimeMillis() + dbBlock.getTimestamp().longValue()*1000;
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
							tx.getDeadline().longValue(),
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
