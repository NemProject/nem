package org.nem.nis.controller;

import net.minidev.json.JSONObject;

import net.minidev.json.JSONValue;
import org.apache.commons.codec.DecoderException;
import org.nem.core.crypto.KeyPair;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.Account;
import org.nem.core.model.Address;
import org.nem.core.model.Message;
import org.nem.core.model.MessageTypes;
import org.nem.core.serialization.BinarySerializer;
import org.nem.core.serialization.Deserializer;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.HexEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.CryptoPrimitive;
import java.util.logging.Logger;

@RestController
public class TransferController {
	private static final Logger logger = Logger.getLogger(NcsMainController.class.getName());

	private String jsonError(int num, String errorMessage) {
		JSONObject obj=new JSONObject();
		obj.put("error", new Integer(num));
		obj.put("reason", errorMessage);
		return obj.toJSONString() + "\r\n";
	}


	@RequestMapping(value="/transfer/prepare", method = RequestMethod.POST)
	public String transferPrepare(@RequestBody String body) {
		logger.info(body);
		JSONObject par = (JSONObject) JSONValue.parse(body);
		logger.info(par.toString());

		String jsonSender;
		String jsonRecipient;
		Long jsonAmount;
		String jsonMessage;

		jsonSender = (String) par.get("sender");
		jsonRecipient = (String) par.get("recipient");
		jsonAmount = (Long) par.get("amount");
		jsonMessage = (String) par.get("message");

		logger.info(jsonSender);
		logger.info(jsonRecipient);
		logger.info(jsonAmount.toString());
		logger.info(jsonMessage);
		logger.warning( Boolean.toString(Address.fromEncoded(jsonRecipient).isValid()) );

		if (! Address.fromEncoded(jsonRecipient).isValid()) {
			return jsonError(2, "Invalid recipient");
		}

		// we don't check upper bound, as network will verify it...
		if (jsonAmount < 0) {
			return jsonError(3, "Negative amount");
		}

		byte[] transcodedMessage = null;
		try {
			transcodedMessage = HexEncoder.getBytes(jsonMessage);

		} catch (DecoderException e) {
			return jsonError(4, "Incorrectly formatted message, rejecting transaction");
		}

		byte[] senderPublicKey = null;
		try {
			senderPublicKey = HexEncoder.getBytes(jsonSender);
		} catch (DecoderException e) {
			return jsonError(5, "Incorrect sender");

		}

		KeyPair sendersKey = new KeyPair(senderPublicKey);
		Account sender = new Account(sendersKey);
		Account recipient = new Account(Address.fromEncoded(jsonRecipient));
		Message message = new PlainMessage(transcodedMessage);

		TransferTransaction transferTransaction = new TransferTransaction(sender, recipient, jsonAmount, message);
		byte[] transferData = transferTransaction.getBytes();

		JSONObject obj=new JSONObject();
		obj.put("data", HexEncoder.getString(transferData));
		return obj.toJSONString() + "\r\n";
	}
}
