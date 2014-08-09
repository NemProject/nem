package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.*;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.dao.ReadOnlyTransferDao;
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
	public AccountMetaDataPair accountGet(@RequestParam(value = "address") final String nemAddress) {
		final Account account = this.accountIo.findByAddress(this.getAddress(nemAddress));
		final AccountMetaData metaData = new AccountMetaData(this.getAccountStatus(account));
		return new AccountMetaDataPair(account, metaData);
	}

	private AccountStatus getAccountStatus(final Account account) {
		return this.foraging.isAccountUnlocked(account)? AccountStatus.UNLOCKED : AccountStatus.LOCKED;
	}

	/**
	 * Unlocks an account for foraging.
	 *
	 * @param privateKey The private key of the account to unlock.
	 */
	@RequestMapping(value = "/account/unlock", method = RequestMethod.POST)
	@ClientApi
	public void accountUnlock(@RequestBody final PrivateKey privateKey) {
		final KeyPair keyPair = new KeyPair(privateKey);
		final Account account = this.accountIo.findByAddress(Address.fromPublicKey(keyPair.getPublicKey()));
		final Account copyOfAccount = account.shallowCopyWithKeyPair(keyPair);
		final UnlockResult result = this.foraging.addUnlockedAccount(copyOfAccount);

		if (UnlockResult.SUCCESS != result)
			throw new IllegalArgumentException(result.toString());
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

	// TODO-CR, with this change, who do we expect to call account/transfers?
	// G-J: noone, account/transfers, should finally be removed and replaced by calls to methods below

	private SerializableList<TransactionMetaDataPair> getAccountTransfersWithHash(AccountTransactionsPageBuilder builder, ReadOnlyTransferDao.TransferType transferType) {
		final AccountTransactionsPage page = builder.build();
		return this.accountIo.getAccountTransfersWithHash(page.getAddress(), page.getHash(), transferType);
	}

	/**
	 * Gets information about transactions of a specified account ending at the specified transaction (via hash).
	 *
	 * @param builder The page builder.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/account/transfers/all", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<TransactionMetaDataPair> accountTransfersAll(final AccountTransactionsPageBuilder builder) {
		return getAccountTransfersWithHash(builder, ReadOnlyTransferDao.TransferType.ALL);
	}

	/**
	 * Gets information about incoming transactions of a specified account ending at the specified transaction (via hash).
	 *
	 * @param builder The page builder.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/account/transfers/incoming", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<TransactionMetaDataPair> accountTransfersIncoming(final AccountTransactionsPageBuilder builder) {
		return getAccountTransfersWithHash(builder, ReadOnlyTransferDao.TransferType.INCOMING);
	}

	/**
	 * Gets information about outgoing transactions of a specified account ending at the specified transaction (via hash).
	 *
	 * @param builder The page builder.
	 * @return Information about the matching transactions.
	 */
	@RequestMapping(value = "/account/transfers/outgoing", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<TransactionMetaDataPair> accountTransfersOutgoing(final AccountTransactionsPageBuilder builder) {
		return getAccountTransfersWithHash(builder, ReadOnlyTransferDao.TransferType.OUTGOING);
	}

	/**
	 * Gets unconfirmed transaction information for the specified account.
	 * TODO: not sure if we should have an AccountPageBuilder here since there isn't paging.
	 *
	 * @param builder The page builder.
	 * @return Information about matching transactions
	 */
	@RequestMapping(value = "/account/unconfirmedTransactions", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Transaction> transactionsUnconfirmed(final AccountPageBuilder builder) {
		final AccountPage page = builder.build();
		return new SerializableList<>(this.foraging.getUnconfirmedTransactions(page.getAddress()));
	}

	/**
	 * Gets information about harvested blocks.
	 *
	 * @param builder The page builder.
	 *
	 * @return information about harvested blocks
	 */
	@RequestMapping(value = "/account/harvests", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<HarvestInfo> accountHarvests(final AccountPageBuilder builder) {
		final AccountPage page = builder.build();
		return this.accountIo.getAccountHarvests(page.getAddress(), page.getTimestamp());
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
	@PublicApi
	@ClientApi
	public SerializableList<AccountImportanceViewModel> getImportances() {
		final List<AccountImportanceViewModel> accounts = StreamSupport.stream(this.accountIo.spliterator(), false)
				.map(a -> new AccountImportanceViewModel(a.getAddress(), a.getImportanceInfo()))
				.collect(Collectors.toList());

		return new SerializableList<>(accounts);
	}
}