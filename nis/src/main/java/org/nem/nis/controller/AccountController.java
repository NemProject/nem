package org.nem.nis.controller;

import org.nem.core.connect.client.NisApiId;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.AccountImportanceViewModel;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.harvesting.*;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.*;

/**
 * REST API for interacting with Account objects.
 */
@RestController
public class AccountController {
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final UnlockedAccounts unlockedAccounts;
	private final AccountIo accountIo;
    private final BlockChainLastBlockLayer blockChainLastBlockLayer;
    private final AccountInfoFactory accountInfoFactory;
	private final PoiFacade poiFacade;

	@Autowired(required = true)
	AccountController(
			final UnconfirmedTransactions unconfirmedTransactions,
			final UnlockedAccounts unlockedAccounts,
			final AccountIo accountIo,
            final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final AccountInfoFactory accountInfoFactory,
			final PoiFacade poiFacade) {
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.unlockedAccounts = unlockedAccounts;
		this.accountIo = accountIo;
        this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.accountInfoFactory = accountInfoFactory;
		this.poiFacade = poiFacade;
	}

	/**
	 * Gets information about an account.
	 *
	 * @param builder The account id builder.
	 * @return The account information.
	 */
	@RequestMapping(value = "/account/get", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaDataPair accountGet(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
        final Long height = this.blockChainLastBlockLayer.getLastBlockHeight();
		final AccountInfo account = this.accountInfoFactory.createInfo(address, new BlockHeight(height));
		final AccountMetaData metaData = new AccountMetaData(this.getAccountStatus(address));
		return new AccountMetaDataPair(account, metaData);
	}

    @RequestMapping(value = "/account/status", method = RequestMethod.GET)
    @ClientApi
    public AccountMetaData accountStatus(final AccountIdBuilder builder) {
        final Address address = builder.build().getAddress();
        return new AccountMetaData(getAccountStatus(address));
    }

    private AccountStatus getAccountStatus(final Address address) {
		return this.unlockedAccounts.isAccountUnlocked(address) ? AccountStatus.UNLOCKED : AccountStatus.LOCKED;
	}

	/**
	 * Unlocks an account for harvesting.
	 *
	 * @param privateKey The private key of the account to unlock.
	 */
	@RequestMapping(value = "/account/unlock", method = RequestMethod.POST)
	@ClientApi
	@TrustedApi
	public void accountUnlock(@RequestBody final PrivateKey privateKey) {
		final KeyPair keyPair = new KeyPair(privateKey);
		final Account account = this.accountIo.findByAddress(Address.fromPublicKey(keyPair.getPublicKey()));
		final Account copyOfAccount = account.shallowCopyWithKeyPair(keyPair);
		final UnlockResult result = this.unlockedAccounts.addUnlockedAccount(copyOfAccount);

		if (UnlockResult.SUCCESS != result) {
			throw new IllegalArgumentException(result.toString());
		}
	}

	/**
	 * Locks an account from harvesting.
	 *
	 * @param privateKey The private key of the account to lock.
	 */
	@RequestMapping(value = "/account/lock", method = RequestMethod.POST)
	@ClientApi
	@TrustedApi
	public void accountLock(@RequestBody final PrivateKey privateKey) {
		final Account account = new Account(new KeyPair(privateKey));
		this.unlockedAccounts.removeUnlockedAccount(account);
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
		return this.getAccountTransfersWithHash(builder, ReadOnlyTransferDao.TransferType.ALL);
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
		return this.getAccountTransfersWithHash(builder, ReadOnlyTransferDao.TransferType.INCOMING);
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
		return this.getAccountTransfersWithHash(builder, ReadOnlyTransferDao.TransferType.OUTGOING);
	}

	private SerializableList<TransactionMetaDataPair> getAccountTransfersWithHash(final AccountTransactionsPageBuilder builder, final ReadOnlyTransferDao.TransferType transferType) {
		final AccountTransactionsPage page = builder.build();
		return this.accountIo.getAccountTransfersWithHash(page.getAddress(), page.getHash(), transferType);
	}

	/**
	 * Gets unconfirmed transaction information for the specified account.
	 *
	 * @param builder The account id builder.
	 * @return Information about matching transactions
	 */
	@RequestMapping(value = "/account/unconfirmedTransactions", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Transaction> transactionsUnconfirmed(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		return new SerializableList<>(this.unconfirmedTransactions.getTransactionsForAccount(address).getAll());
	}

	/**
	 * Gets information about harvested blocks.
	 *
	 * @param builder The page builder.
	 * @return information about harvested blocks
	 */
	@RequestMapping(value = "/account/harvests", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<HarvestInfo> accountHarvests(final AccountTransactionsPageBuilder builder) {
		final AccountTransactionsPage page = builder.build();
		return this.accountIo.getAccountHarvests(page.getAddress(), page.getHash());
	}

	/**
	 * Gets the current account importance information for all accounts.
	 *
	 * @return Account importance information.
	 */
	@RequestMapping(value = "/account/importances", method = RequestMethod.GET)
	@PublicApi
	public SerializableList<AccountImportanceViewModel> getImportances() {
		final List<AccountImportanceViewModel> viewModels = StreamSupport.stream(this.poiFacade.spliterator(), false)
				.map(a -> new AccountImportanceViewModel(a.getAddress(), a.getImportanceInfo()))
				.collect(Collectors.toList());

		return new SerializableList<>(viewModels);
	}
}