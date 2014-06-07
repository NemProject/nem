package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.Foraging;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.service.AccountIo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.*;

/**
 * REST API for interacting with Account objects.
 */
@RestController
public class AccountController {
	private final Foraging foraging;
	private final AccountIo accountIo;

	@Autowired(required = true)
	AccountController(final Foraging foraging, final AccountIo accountIo) {
		this.foraging = foraging;
		this.accountIo = accountIo;
	}

	@RequestMapping(value = "/account/get", method = RequestMethod.GET)
	@ClientApi
	public Account accountGet(@RequestParam(value = "address") final String nemAddress) {
		return this.accountIo.findByAddress(getAddress(nemAddress));
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

	/**
	 * Locks an account from foraging.
	 *
	 * @param privateKey The private key of the account to lock.
	 */
	@RequestMapping(value = "/account/lock", method = RequestMethod.POST)
	@ClientApi
	public void accountLock(@RequestBody final PrivateKey privateKey) {
		final Account account = new Account(new KeyPair(privateKey));
		this.foraging.removeUnlockedAccount(account);
	}

	/**
	 * Gets transaction information for the specified account starting at the specified time.
	 *
	 * @param builder The page builder.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/account/transfers", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<TransactionMetaDataPair> accountTransfers(final AccountPageBuilder builder) {
		final AccountPage page = builder.build();
		return this.accountIo.getAccountTransfers(page.getAddress(), page.getTimestamp());
	}

	/**
	 * Gets block information for the specified account starting at the specified time.
	 *
	 * @param builder The page builder.
	 * @return Information about the matching blocks.
	 */
	@RequestMapping(value = "/account/blocks", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Block> accountBlocks(final AccountPageBuilder builder) {
		final AccountPage page = builder.build();
		return this.accountIo.getAccountBlocks(page.getAddress(), page.getTimestamp());
	}

	private Address getAddress(final String nemAddress) {
		Address address = Address.fromEncoded(nemAddress);
		if (!address.isValid()) {
			throw new IllegalArgumentException("address is not valid");
		}

		return address;
	}

	/**
	 * Gets the current account importance information for all accounts.
	 *
	 * @return Account importance information.
	 */
	@RequestMapping(value = "/account/importances", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<AccountImportanceViewModel> getImportances() {
		final List<AccountImportanceViewModel> accounts = StreamSupport.stream(this.accountIo.spliterator(), false)
				.map(a -> new AccountImportanceViewModel(a.getAddress(), a.getImportanceInfo()))
				.collect(Collectors.toList());

		return new SerializableList<>(accounts);
	}
}