package org.nem.nis;

import java.util.*;
import java.util.logging.Logger;

import org.nem.core.crypto.KeyPair;
import org.nem.core.dao.BlockDao;
import org.nem.core.dao.TransferDao;
import org.nem.core.dbmodel.Block;
import org.nem.core.dbmodel.Transfer;
import org.nem.core.model.Account;
import org.nem.core.model.Address;
import org.nem.core.serialization.AccountLookup;
import org.springframework.beans.factory.annotation.Autowired;

public class AccountAnalyzer implements AccountLookup {
	private static final Logger LOGGER = Logger.getLogger(AccountAnalyzer.class.getName());

	@Autowired
	private BlockDao blockDao;
	
	@Autowired
	private TransferDao transferDao;

	Map<byte[], Account> mapByPublicKey;
	Map<String, Account> mapByAddressId;

	public AccountAnalyzer() {
		mapByPublicKey = new HashMap<>();
		mapByAddressId = new HashMap<>();
	}

	private Account addToBalanceAndUnconfirmedBalance(org.nem.core.dbmodel.Account a, long amount) {
		byte[] publicKey = a.getPublicKey();
		String encodedAddress = a.getPrintableKey();

		Account account = findByAddressImpl(publicKey, encodedAddress);
		if (account == null) {
			if (publicKey != null) {
				account = new Account(new KeyPair(publicKey));

				mapByPublicKey.put(publicKey, account);

				// in case of genesis account we didn't knew it's //encoded address//
				// earlier, so additionally insert it in mapByAddressId
				if (encodedAddress.equals(Genesis.CREATOR_ACCOUNT_ID)) {
					mapByAddressId.put(encodedAddress, account);
				}

			} else {
				account = new Account(Address.fromEncoded(encodedAddress));
				mapByAddressId.put(encodedAddress, account);
			}
		}

		account.incrementBalance(amount);
		return account;
	}

	/*
	 * analyze block from db
	 * 
	 * if we're here it means that both block an it's transactions
	 * have been saved in db
	 *
	 * Currently it analyzes ONLY "transfers"
	 */
	public void analyze(Block curBlock) {
		List<Transfer> txes = curBlock.getBlockTransfers();
		LOGGER.info("analyzing block: " + Long.toString(curBlock.getShortId()) + ", #tx " + Integer.toString(txes.size()));

		// add fee's to block forger
		//

		for(Iterator<Transfer> i = txes.iterator(); i.hasNext(); ) {
			Transfer tx = i.next();

			addToBalanceAndUnconfirmedBalance(tx.getSender(), -(tx.getAmount() + tx.getFee()));
			Account recipient = addToBalanceAndUnconfirmedBalance(tx.getRecipient(), tx.getAmount());

			LOGGER.info(String.format("%s + %d [fee: %d]", recipient.getAddress().getEncoded(), tx.getAmount(), tx.getFee()));
		}
	}

	/**
	 * Finds an account, updating it's public key if there's a need.
	 *
	 * @param publicKey - public key of an account, might be null
	 * @param encodedAddress - encoded address of an account
	 * @return null if account is unknown or Account associated with an address
	 */
	protected Account findByAddressImpl(byte[] publicKey, String encodedAddress) {
		// if possible return by public key
		if (publicKey != null) {
			if (mapByPublicKey.containsKey(publicKey)) {
				return mapByPublicKey.get(publicKey);
			}
		}

		// otherwise try to return by address
		if (mapByAddressId.containsKey(encodedAddress)) {
			Account oldAccount = mapByAddressId.get(encodedAddress);

			// if possible update account's public key
			if (publicKey != null) {
				Account account = new Account(new KeyPair(publicKey));
				long balance = oldAccount.getBalance();
				account.incrementBalance(balance);
				mapByAddressId.put(encodedAddress, account);

				// associate public key with an account
				mapByPublicKey.put(publicKey, account);
			}

			return mapByAddressId.get(encodedAddress);
		}

		return null;
	}

	/**
	 * Finds an account, updating it's public key if there's a need.
	 *
	 * @param id - Address of an account
	 *
	 * @return Account associated with an address or new Account if address was unknown
	 */
	@Override
	public Account findByAddress(Address id) {
		LOGGER.info("looking for [" + id.getEncoded() + "]" + Integer.toString(mapByAddressId.size()));

		if (! id.isValid()) {
			throw new MissingResourceException("invalid address: ", Address.class.getName(), id.getEncoded());
		}

		Account account = findByAddressImpl(id.getPublicKey(), id.getEncoded());

		// we don't know it yet, so create dummy account
		// without adding it anywhere yet
		if (account == null) {
			if (id.getPublicKey() != null) {
				account = new Account(new KeyPair(id.getPublicKey()));

			} else {
				account = new Account(Address.fromEncoded(id.getEncoded()));
			}
		}

		return account;
	}
}