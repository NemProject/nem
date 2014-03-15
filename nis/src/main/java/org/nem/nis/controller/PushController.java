package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.nem.core.model.Block;
import org.nem.core.model.BlockFactory;
import org.nem.core.model.Transaction;
import org.nem.core.model.VerifiableEntity;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.transactions.TransactionFactory;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.nem.peer.NodeApiId;
import org.nem.peer.PeerNetworkHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.InvalidParameterException;
import java.util.MissingResourceException;
import java.util.logging.Logger;

/**
 * This controller will handle data propagation:
 *  * /push/transaction - for what is now model.Transaction
 *  * /push/block - for model.Block
 *
 *  It would probably fit better in TransferController, but this is
 *  part of p2p API, so I think it should be kept separated.
 *  (I think it might pay off in future, if we'd like to add restrictions to client APIs)
 */

@RestController
public class PushController {
	private static final Logger LOGGER = Logger.getLogger(NcsMainController.class.getName());

	@Autowired
	AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockChain blockChain;

	@RequestMapping(value="/push/transaction", method = RequestMethod.POST)
	public String pushTransaction(@RequestBody String body)
	{
		JSONObject par;
		try {
			par = (JSONObject) JSONValue.parse(body);

		} catch (ClassCastException e) {
			return Utils.jsonError(1, "invalid json");
		}
		LOGGER.info(par.toString());

		JsonDeserializer deserializer = new JsonDeserializer(par, new DeserializationContext(accountAnalyzer));
		Transaction transaction;
		try {
			transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

		} catch (MissingResourceException|InvalidParameterException|NullPointerException e) {
			return Utils.jsonError(1, "incorrect data");
		}

		LOGGER.info("   signer: " + HexEncoder.getString(transaction.getSigner().getKeyPair().getPublicKey()));
		LOGGER.info("   verify: " + Boolean.toString(transaction.verify()));

		// transaction timestamp is checked inside processTransaction
		if (transaction.isValid() && transaction.verify()) {
			PeerNetworkHost peerNetworkHost = PeerNetworkHost.getDefaultHost();

			// add to unconfirmed transactions
			if (blockChain.processTransaction(transaction)) {
				peerNetworkHost.getNetwork().broadcast(NodeApiId.REST_PUSH_TRANSACTION, transaction);
			}
			return Utils.jsonOk();
		}

		return Utils.jsonError(2, "transaction couldn't be verified " + Boolean.toString(transaction.verify()));
	}

	@RequestMapping(value="/push/block", method = RequestMethod.POST)
	public String pushBlock(@RequestBody String body)
	{
		JSONObject par;
		try {
			par = (JSONObject) JSONValue.parse(body);

		} catch (ClassCastException e) {
			return Utils.jsonError(1, "invalid json");
		}
		LOGGER.info(par.toString());

		JsonDeserializer deserializer = new JsonDeserializer(par, new DeserializationContext(accountAnalyzer));
		Block block;
		try {
			block = BlockFactory.VERIFIABLE.deserialize(deserializer);

		} catch (MissingResourceException|InvalidParameterException|NullPointerException e) {
			return Utils.jsonError(1, "incorrect data");
		}

		LOGGER.info("   signer: " + HexEncoder.getString(block.getSigner().getKeyPair().getPublicKey()));
		LOGGER.info("   verify: " + Boolean.toString(block.verify()));

		if (block.verify()) {
			PeerNetworkHost peerNetworkHost = PeerNetworkHost.getDefaultHost();

			// validate block, add to chain
			blockChain.processBlock(block);

			// TODO: propagate block
			//peerNetworkHost.getNetwork().broadcast(NodeApiId.REST_PUSH_BLOCK, block);
			return Utils.jsonOk();
		}

		return Utils.jsonError(2, "block couldn't be verified " + Boolean.toString(block.verify()));
	}
}
