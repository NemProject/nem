package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

@Service
public class AccountIoAdapter implements AccountIo {
	private final RequiredTransferDao transferDao;
	private final RequiredBlockDao blockDao;
	private final AccountAnalyzer accountAnalyzer;

	@Autowired(required = true)
	public AccountIoAdapter(
			final RequiredTransferDao transferDao,
			final RequiredBlockDao blockDao,
			final AccountAnalyzer accountAnalyzer) {
		this.transferDao = transferDao;
		this.blockDao = blockDao;
		this.accountAnalyzer = accountAnalyzer;
	}

	@Override
	public Account findByAddress(Address address) {
		return this.accountAnalyzer.findByAddress(address);
	}

	private Integer intOrMaxInt(String timestamp) {
		Integer intTimestamp;
		if (timestamp == null) {
			return Integer.MAX_VALUE;
		}
		try {
			intTimestamp = Integer.valueOf(timestamp, 10);
		} catch (NumberFormatException e) {
			intTimestamp = Integer.MAX_VALUE;
		}
		return intTimestamp;
	}

	@Override
	public SerializableList<TransactionMetaDataPair> getAccountTransfers(final Address address, final String timestamp) {

		// TODO: probably it'll be better to a) ask accountDao about account
		// TODO: b) pass obtained db-account to getTransactionsForAccount

		final Account account = this.accountAnalyzer.findByAddress(address);
		final Integer intTimestamp = intOrMaxInt(timestamp);
		final Collection<Object[]> transfers = this.transferDao.getTransactionsForAccount(account, intTimestamp, 25);

		final SerializableList<TransactionMetaDataPair> transactionList = new SerializableList<>(0);
		transfers.stream()
				.map(tr -> new TransactionMetaDataPair(
						TransferMapper.toModel((Transfer)tr[0], this.accountAnalyzer),
						new TransactionMetaData(new BlockHeight((long)tr[1]))
				))
				.forEach(obj -> transactionList.add(obj));
		return transactionList;
	}

	@Override
	public SerializableList<Block> getAccountBlocks(final Address address, final String timestamp) {
		final Account account = this.accountAnalyzer.findByAddress(address);
		final Integer intTimestamp = intOrMaxInt(timestamp);
		Collection<org.nem.nis.dbmodel.Block> blocks = blockDao.getBlocksForAccount(account, intTimestamp, 25);

		final SerializableList<Block> blockList = new SerializableList<>(0);
		blocks.stream()
				.forEach(bl -> bl.setBlockTransfers(new LinkedList<>()));

		blocks.stream()
				.map(bl -> BlockMapper.toModel(bl, this.accountAnalyzer))
				.forEach(obj -> blockList.add(obj));
		return blockList;
	}

	@Override
	public Iterator<Account> iterator() {
		return this.accountAnalyzer.iterator();
	}
}
