package org.nem.nis.controller;

import net.minidev.json.JSONObject;

import net.minidev.json.JSONValue;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.transactions.TransactionFactory;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.DbAccountLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@RestController
public class TransferController {
	private static final Logger logger = Logger.getLogger(NcsMainController.class.getName());

	@Autowired
	DbAccountLookup dbAccountLookup;

	private String jsonError(int num, String errorMessage) {
		JSONObject obj=new JSONObject();
		obj.put("error", new Integer(num));
		obj.put("reason", errorMessage);
		return obj.toJSONString() + "\r\n";
	}


	@RequestMapping(value="/transfer/prepare", method = RequestMethod.POST)
	public String transferPrepare(@RequestBody String body) {
		JSONObject par = (JSONObject)JSONValue.parse(body);
		logger.info(par.toString());

		JsonDeserializer deserializer = new JsonDeserializer(par, new DeserializationContext(dbAccountLookup));
		TransferTransaction transferTransaction;
		try {
			transferTransaction = (TransferTransaction) TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);

		} catch (NullPointerException e) {
			return jsonError(1, "incorrect data");
		}

		byte[] transferData = BinarySerializer.serializeToBytes(transferTransaction);

		JSONObject obj = JsonSerializer.serializeToJson(new RequestPrepare(transferData));
		return obj.toJSONString() + "\r\n";
	}

	@RequestMapping(value="/transfer/announce", method = RequestMethod.POST)
	public String transferAnnounce(@RequestBody String body) {
		JSONObject par = (JSONObject) JSONValue.parse(body);
		logger.info(par.toString());

		JsonDeserializer jsonDeserializer = new JsonDeserializer(par, null);
		RequestAnnounce requestAnnounce = new RequestAnnounce(jsonDeserializer);

		BinaryDeserializer deserializer = new BinaryDeserializer(requestAnnounce.getData(), new DeserializationContext(dbAccountLookup));
		TransferTransaction transaction = new TransferTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
		transaction.setSignature(new Signature(requestAnnounce.getSignature()));

		logger.info("   signer: " + HexEncoder.getString(transaction.getSigner().getKeyPair().getPublicKey()));
		logger.info("recipient: " + transaction.getRecipient().getAddress().getEncoded());
		logger.info("   verify: " + Boolean.toString(transaction.verify()));

		return jsonError(2, "All ok sending transaction to network : verified " + Boolean.toString(transaction.verify()));
	}
}