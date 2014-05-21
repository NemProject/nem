package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.ncc.TransactionMetaData;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.mappers.TransferMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedList;

@Service
public class AccountIoAdapter implements AccountIo {
	private final RequiredTransferDao transferDao;
	private final RequiredBlockDao blockDao;
	private final AccountLookup accountLookup;

	@Autowired(required = true)
	public AccountIoAdapter(final RequiredTransferDao transferDao, final RequiredBlockDao blockDao, final AccountLookup accountLookup) {
		this.transferDao = transferDao;
		this.blockDao = blockDao;
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

		final Collection<Object[]> transfers = this.transferDao.getTransactionsForAccount(account, 25);

		final SerializableList<TransactionMetaDataPair> transactionList = new SerializableList<>(0);
		transfers.stream()
				.map(tr -> new TransactionMetaDataPair(
						TransferMapper.toModel((Transfer)tr[0], this.accountLookup),
						new TransactionMetaData(new BlockHeight((long)tr[1]))
				))
				.forEach(transactionList::add);
		return transactionList;
	}

	@Override
	public SerializableList<Block> getAccountBlocks(Address address) {
		final Account account = this.accountLookup.findByAddress(address);
		Collection<org.nem.nis.dbmodel.Block> blocks = blockDao.getBlocksForAccount(account, 25);

		final SerializableList<Block> blockList = new SerializableList<>(0);
		blocks.stream()
				.forEach(bl -> bl.setBlockTransfers(new LinkedList<>()));

		blocks.stream()
				.map(bl -> BlockMapper.toModel(bl, this.accountLookup))
				.forEach(blockList::add);
		return blockList;
	}
}
