package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.mappers.TransferMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.MissingResourceException;

@Service
public class AccountIoAdapter implements AccountIo {
	private final TransferDao transferDao;
	private final AccountLookup accountLookup;

	@Autowired(required = true)
	public AccountIoAdapter(final TransferDao transferDao, final AccountLookup accountLookup) {
		this.transferDao = transferDao;
		this.accountLookup = accountLookup;
	}

	@Override
	public Account findByAddress(Address address) {
		return this.accountLookup.findByAddress(address);
	}

	@Override
	public SerializableList<Transaction> getAccountTransfers(Address address) {

		// TODO: probably it'll be better to a) ask accountDao about account
		// TODO: b) pass obtained db-account to getTransactionsForAccount

		final Account account = this.accountLookup.findByAddress(address);

		Collection<Transfer> transfers = transferDao.getTransactionsForAccount(account, 25);

		final SerializableList<Transaction> transactionList = new SerializableList<>(0);
		transfers.stream().map(tr -> TransferMapper.toModel(tr, this.accountLookup)).forEach(
				transaction -> transactionList.add(transaction)
		);
		return transactionList;
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("account not found", AccountIoAdapter.class.getName(), key);
	}
}
