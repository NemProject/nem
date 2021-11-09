package org.nem.nis.controller;

import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.nis.controller.annotations.P2PApi;
import org.nem.nis.service.PushService;
import org.nem.peer.SecureSerializableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * This controller will handle data propagation: <br>
 * * /push/transaction - for what is now model.Transaction <br>
 * * /push/block - for model.Block
 */
@RestController
public class PushController {
	private static final Logger LOGGER = Logger.getLogger(PushController.class.getName());
	private final PushService pushService;

	@Autowired(required = true)
	public PushController(final PushService pushService) {
		this.pushService = pushService;
	}

	/**
	 * Pushes a transaction to other nodes.
	 *
	 * @param deserializer The deserializer that should create a SecureSerializableEntity Transaction.
	 */
	@RequestMapping(value = "/push/transaction", method = RequestMethod.POST)
	@P2PApi
	public void pushTransaction(@RequestBody final Deserializer deserializer) {
		final SecureSerializableEntity<Transaction> secureEntity = new SecureSerializableEntity<>(deserializer,
				TransactionFactory.VERIFIABLE);
		this.pushService.pushTransaction(secureEntity.getEntity(), secureEntity.getIdentity());
	}

	/**
	 * Pushes a list of transactions to other nodes.
	 *
	 * @param deserializer The deserializer that should create a SecureSerializableEntity list of transactions.
	 */
	@RequestMapping(value = "/push/transactions", method = RequestMethod.POST)
	@P2PApi
	public void pushTransactions(@RequestBody final Deserializer deserializer, HttpServletRequest request) {
		final SerializableList<SecureSerializableEntity<Transaction>> serializableList = new SerializableList<>(deserializer,
				d -> new SecureSerializableEntity<>(d, TransactionFactory.VERIFIABLE));
		// TODO 20151024 J-B: is this going to be too noisy in the logs?
		LOGGER.info(String.format("received %d transactions from %s", serializableList.size(), request.getRemoteHost()));

		// could optimize this if needed
		serializableList.asCollection().stream().forEach(e -> this.pushService.pushTransaction(e.getEntity(), e.getIdentity()));
	}

	/**
	 * Pushes a block to other nodes.
	 *
	 * @param deserializer The deserializer that should create a SecureSerializableEntity Block.
	 */
	@RequestMapping(value = "/push/block", method = RequestMethod.POST)
	@P2PApi
	public void pushBlock(@RequestBody final Deserializer deserializer) {
		final SecureSerializableEntity<Block> secureEntity = new SecureSerializableEntity<>(deserializer, BlockFactory.VERIFIABLE);
		this.pushService.pushBlock(secureEntity.getEntity(), secureEntity.getIdentity());
	}
}
