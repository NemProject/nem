package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.serialization.Deserializer;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountController {

	@Autowired
	AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockChain blockChain;

	@RequestMapping(value="/account/unlock", method = RequestMethod.POST)
	public String accountUnlock(@RequestBody final String body) {
        final Deserializer deserializer = ControllerUtils.getDeserializer(body, this.accountAnalyzer);
        final Account account = new Account(new KeyPair(new PrivateKey(deserializer)));
        blockChain.addUnlockedAccount(account);
		return Utils.jsonOk();
	}
}
