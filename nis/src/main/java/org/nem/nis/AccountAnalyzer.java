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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

public class AccountAnalyzer implements AccountLookup {
	private static final Logger logger = Logger.getLogger(AccountAnalyzer.class.getName());

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

		Account account;
		if (publicKey != null) {
			if (! mapByPublicKey.containsKey(publicKey)) {
				account = new Account(new KeyPair(publicKey));
				mapByPublicKey.put(publicKey, account);

				String accountId = account.getAddress().getEncoded();
				// in case of genesis account we must 'insert'
				if (accountId.equals(Genesis.CREATOR_ACCOUNT_ID)) {
					mapByAddressId.put(accountId, account);

				// in case of other accounts, simply update the field
				// not to loose the amounts...
				} else {

					// not sure, probably mapByAddressId.get(accountId).setKeyPair(account.getKeyPair)
					// would be better
					long balance = mapByAddressId.get(accountId).getBalance();
					account.incrementBalance(balance);
					mapByAddressId.put(accountId, account);
				}

			} else {
				account = mapByPublicKey.get(publicKey);
			}

		} else {
			String addressId = a.getPrintableKey();
			if (!mapByAddressId.containsKey(addressId)) {
				account = new Account(Address.fromEncoded(a.getPrintableKey()));
				mapByAddressId.put(account.getAddress().getEncoded(), account);

			} else {
				account = mapByAddressId.get(addressId);
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
		System.out.print("analyzing block: ");
		System.out.print(curBlock.getShortId());
		System.out.print(", #tx ");

		List<Transfer> txes = curBlock.getBlockTransfers();
		System.out.println(txes.size());

		// add fee's to block forger
		//

		for(Iterator<Transfer> i = txes.iterator(); i.hasNext(); ) {
			Transfer tx = i.next();

			addToBalanceAndUnconfirmedBalance(tx.getSender(), -(tx.getAmount() + tx.getFee()));
			Account recipient = addToBalanceAndUnconfirmedBalance(tx.getRecipient(), tx.getAmount());

			System.out.println(String.format("%s + %d [fee: %d]", recipient.getAddress().getEncoded(), tx.getAmount(), tx.getFee()));
		}
	}

	@Override
	public org.nem.core.model.Account findByAddress(Address id) {
		logger.info("looking for [" + id.getEncoded() + "]" + Integer.toString(mapByAddressId.size()));

		if (mapByAddressId.containsKey(id.getEncoded())) {
			logger.info("found");
			return mapByAddressId.get(id.getEncoded());
		}

		throw new MissingResourceException("account not found in the db", Address.class.getName(), id.getEncoded());

		// TODO: will we have separate APIs: findByAddress + findBy (issue/11)
	}
}
