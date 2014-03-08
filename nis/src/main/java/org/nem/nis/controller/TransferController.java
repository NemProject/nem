package org.nem.nis.controller;

import net.minidev.json.JSONObject;

import net.minidev.json.JSONValue;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.transactions.TransactionFactory;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
//import org.nem.peer.v2.PeerNetwork;
import org.nem.peer.PeerNetworkHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.InvalidParameterException;
import java.util.MissingResourceException;
import java.util.logging.Logger;

@RestController
public class TransferController {
	private static final Logger logger = Logger.getLogger(NcsMainController.class.getName());

	@Autowired
	AccountAnalyzer accountAnalyzer;

	private String jsonError(int num, String errorMessage) {
		JSONObject obj=new JSONObject();
		obj.put("error", new Integer(num));
		obj.put("reason", errorMessage);
		return obj.toJSONString() + "\r\n";
	}

	private String jsonOk() {
		JSONObject obj=new JSONObject();
		obj.put("ok", 42);
		return obj.toJSONString() + "\r\n";
	}


	@RequestMapping(value="/transfer/prepare", method = RequestMethod.POST)
	public String transferPrepare(@RequestBody String body) {
		JSONObject par;
		try {
			par = (JSONObject)JSONValue.parse(body);

		} catch (ClassCastException e) {
			return jsonError(1, "invalid json");
		}
		logger.info(par.toString());

		logger.info("threadId : " + Long.toString(Thread.currentThread().getId()) );

		JsonDeserializer deserializer = new JsonDeserializer(par, new DeserializationContext(accountAnalyzer));
		TransferTransaction transferTransaction;
		try {
			transferTransaction = (TransferTransaction) TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);

		// not found in db
		} catch (MissingResourceException e) {
			return jsonError(1, "incorrect data");

		// wrong transaction type
		} catch (InvalidParameterException e) {
			return jsonError(1, "incorrect data");

		//
		} catch (NullPointerException e) {
			return jsonError(1, "incorrect data");
		}

		if (! transferTransaction.isValid()) {
			return jsonError(1, "incorrect data");
		}

		BinarySerializer binarySerializer = new BinarySerializer();
		transferTransaction.asNonVerifiable().serialize(binarySerializer);
		byte[] transferData = binarySerializer.getBytes();

		JSONObject obj = JsonSerializer.serializeToJson(new RequestPrepare(transferData));
		return obj.toJSONString() + "\r\n";
	}

	@RequestMapping(value="/transfer/announce", method = RequestMethod.POST)
	public String transferAnnounce(@RequestBody String body) {
		JSONObject par = (JSONObject) JSONValue.parse(body);
		logger.info(par.toString());

		JsonDeserializer jsonDeserializer = new JsonDeserializer(par, null);
		RequestAnnounce requestAnnounce = new RequestAnnounce(jsonDeserializer);

		BinaryDeserializer deserializer = new BinaryDeserializer(requestAnnounce.getData(), new DeserializationContext(accountAnalyzer));
		TransferTransaction transaction = (TransferTransaction)TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);
		transaction.setSignature(new Signature(requestAnnounce.getSignature()));

		logger.info("   signer: " + HexEncoder.getString(transaction.getSigner().getKeyPair().getPublicKey()));
		logger.info("recipient: " + transaction.getRecipient().getAddress().getEncoded());
		logger.info("   verify: " + Boolean.toString(transaction.verify()));

		if (transaction.isValid() && transaction.verify()) {
			PeerNetworkHost peerNetworkHost = PeerNetworkHost.getDefaultHost();
			//peerNetworkHost.getNetwork().announceTransaction(transaction);
			return jsonOk();
		}
		return jsonError(2, "transaction couldn't be verified " + Boolean.toString(transaction.verify()));
	}
}