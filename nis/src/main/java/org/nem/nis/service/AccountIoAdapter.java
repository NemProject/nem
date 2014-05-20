package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.TransactionMetaData;
import org.nem.core.model.ncc.TransactionMetaDataPair;
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
	private final RequiredTransferDao transferDao;
	private final AccountLookup accountLookup;

	@Autowired(required = true)
	public AccountIoAdapter(final RequiredTransferDao transferDao, final AccountLookup accountLookup) {
		this.transferDao = transferDao;
		this.accountLookup = accountLookup;
	}

	@Override
	public Account findByAddress(Address address) {
		return this.accountLookup.findByAddress(address);
	}

	@Override
	public SerializableList<TransactionMetaDataPair> getAccountTransfers(Address address) {

		// TODO: probably it'll be better to a) ask accountDao about account
		// TODO: b) pass obtained db-account to getTransactionsForAccount

		final Account account = this.accountLookup.findByAddress(address);

		Collection<Object[]> transfers = transferDao.getTransactionsForAccount(account, 25);

		final SerializableList<TransactionMetaDataPair> transactionList = new SerializableList<>(0);
		transfers.stream()
				.map(tr -> new TransactionMetaDataPair(
						TransferMapper.toModel((Transfer)tr[0], this.accountLookup),
						new TransactionMetaData(new BlockHeight((long)tr[1]))
				))
				.forEach(transactionList::add);
		return transactionList;
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("account not found", AccountIoAdapter.class.getName(), key);
	}
}
