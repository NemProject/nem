package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.Account;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.MissingResourceException;
import java.util.logging.Logger;

@RestController
public class AccountController {
	private static final Logger LOGGER = Logger.getLogger(NcsMainController.class.getName());

	@Autowired
	AccountAnalyzer accountAnalyzer;

	@RequestMapping(value="/account/unlock", method = RequestMethod.POST)
	public String accountUnlock(@RequestBody String body) {
		JSONObject par;
		try {
			par = (JSONObject) JSONValue.parse(body);

		} catch (ClassCastException e) {
			return Utils.jsonError(1, "invalid json");
		}
		LOGGER.info(par.toString());

		JsonDeserializer deserializer = new JsonDeserializer(par, new DeserializationContext(accountAnalyzer));
		Account account;
		try {
			byte[] privatekey = deserializer.readBytes("privatekey");
			account = new Account(new KeyPair(new BigInteger(privatekey)));
			BlockChain.MAIN_CHAIN.addUnlockedAccount(account);

		} catch (MissingResourceException |InvalidParameterException |NullPointerException e) {
			return Utils.jsonError(1, "incorrect data");
		}

		return Utils.jsonOk();
	}
}
