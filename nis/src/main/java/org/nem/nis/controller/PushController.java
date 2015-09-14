package org.nem.nis.controller;

import org.nem.core.model.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.P2PApi;
import org.nem.nis.service.PushService;
import org.nem.peer.SecureSerializableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * This controller will handle data propagation:
 * * /push/transaction - for what is now model.Transaction
 * * /push/block - for model.Block
 * <br>
 * It would probably fit better in TransferController, but this is
 * part of p2p API, so I think it should be kept separated.
 * (I think it might pay off in future, if we'd like to add restrictions to client APIs)
 */
@RestController
public class PushController {
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
		final SecureSerializableEntity<Transaction> secureEntity = new SecureSerializableEntity<>(deserializer, TransactionFactory.VERIFIABLE);
		this.pushService.pushTransaction(secureEntity.getEntity(), secureEntity.getIdentity());
	}

	/**
	 * Pushes a list of transactions to other nodes.
	 *
	 * @param deserializer The deserializer that should create a SecureSerializableEntity list of transactions.
	 */
	@RequestMapping(value = "/push/transactions", method = RequestMethod.POST)
	@P2PApi
	public void pushTransactions(@RequestBody final Deserializer deserializer) {
		final SecureSerializableEntity<SerializableList<Transaction>> secureEntity =
				new SecureSerializableEntity<>(deserializer, d -> new SerializableList<>(d, TransactionFactory.VERIFIABLE));

		// could optimize this if needed
		secureEntity.getEntity().asCollection().stream().forEach(t -> this.pushService.pushTransaction(t, secureEntity.getIdentity()));
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
