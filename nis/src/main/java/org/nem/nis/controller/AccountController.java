package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.nis.Foraging;
import org.nem.nis.controller.annotations.ClientApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for interacting with Account objects.
 */
@RestController
public class AccountController {

	private final Foraging foraging;

	@Autowired(required = true)
	AccountController(final Foraging foraging) {
		this.foraging = foraging;
	}

	/**
	 * Unlocks an account for foraging.
	 *
	 * @param privateKey The private key of the account to unlock.
	 */
	@RequestMapping(value = "/account/unlock", method = RequestMethod.POST)
	@ClientApi
	public void accountUnlock(@RequestBody final PrivateKey privateKey) {
		final Account account = new Account(new KeyPair(privateKey));
		this.foraging.addUnlockedAccount(account);
	}
}