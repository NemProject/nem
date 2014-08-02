package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.time.TimeInstant;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.mappers.TransferMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
	public SerializableList<HarvestInfo> getAccountHarvests(Address address, String timestamp) {
		final Account account = this.accountAnalyzer.findByAddress(address);
		final Integer intTimestamp = intOrMaxInt(timestamp);
		Collection<org.nem.nis.dbmodel.Block> blocks = blockDao.getBlocksForAccount(account, intTimestamp, 25);

		final SerializableList<HarvestInfo> blockList = new SerializableList<>(0);

		blocks.stream()
				.map(bl -> new HarvestInfo(bl.getBlockHash(), new BlockHeight(bl.getHeight()), new TimeInstant(bl.getTimestamp()), Amount.fromMicroNem(bl.getTotalFee())))
				.forEach(obj -> blockList.add(obj));
		return blockList;
	}

	@Override
	public Iterator<Account> iterator() {
		return this.accountAnalyzer.iterator();
	}
}
