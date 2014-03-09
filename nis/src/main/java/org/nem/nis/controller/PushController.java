package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.nem.core.model.Transaction;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.transactions.TransactionFactory;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
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
	private static final Logger logger = Logger.getLogger(NcsMainController.class.getName());

	@Autowired
	AccountAnalyzer accountAnalyzer;

	@Autowired
	BlockChain blockChain;

	@RequestMapping(value="/push/transaction", method = RequestMethod.POST)
	public String pushEntity(@RequestBody String body)
	{
		JSONObject par;
		try {
			par = (JSONObject) JSONValue.parse(body);

		} catch (ClassCastException e) {
			return Utils.jsonError(1, "invalid json");
		}
		logger.info(par.toString());

		JsonDeserializer deserializer = new JsonDeserializer(par, new DeserializationContext(accountAnalyzer));
		Transaction transaction;
		try {
			transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

		} catch (MissingResourceException|InvalidParameterException|NullPointerException e) {
			return Utils.jsonError(1, "incorrect data");
		}

		logger.info("   signer: " + HexEncoder.getString(transaction.getSigner().getKeyPair().getPublicKey()));
		logger.info("   verify: " + Boolean.toString(transaction.verify()));

		if (transaction.isValid() && transaction.verify()) {
			PeerNetworkHost peerNetworkHost = PeerNetworkHost.getDefaultHost();

			// add to unconfirmed transactions
			blockChain.processTransaction(transaction);

			// TODO: propagate transactions
			//peerNetworkHost.getNetwork().announceTransaction(transaction);
			return Utils.jsonOk();
		}

		return Utils.jsonError(2, "transaction couldn't be verified " + Boolean.toString(transaction.verify()));
	}
}
