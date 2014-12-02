package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.time.TimeInstant;
import org.nem.nis.AccountCache;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.TransferBlockPair;
import org.nem.nis.mappers.TransferMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AccountIoAdapter implements AccountIo {
	private static final int DEFAULT_LIMIT = 25;

	private final ReadOnlyTransferDao transferDao;
	private final ReadOnlyBlockDao blockDao;
	private final AccountCache accountCache;

	@Autowired(required = true)
	public AccountIoAdapter(
			final ReadOnlyTransferDao transferDao,
			final ReadOnlyBlockDao blockDao,
			final AccountCache accountCache) {
		this.transferDao = transferDao;
		this.blockDao = blockDao;
		this.accountCache = accountCache;
	}

	@Override
	public Account findByAddress(final Address address) {
		return this.accountCache.findByAddress(address);
	}

	private Integer intOrMaxInt(final String timeStamp) {
		Integer intTimeStamp;
		if (timeStamp == null) {
			return Integer.MAX_VALUE;
		}
		try {
			intTimeStamp = Integer.valueOf(timeStamp, 10);
		} catch (final NumberFormatException e) {
			intTimeStamp = Integer.MAX_VALUE;
		}
		return intTimeStamp;
	}

	// TODO-CR: might make sense to add a test for at least the new code

	@Override
	public SerializableList<TransactionMetaDataPair> getAccountTransfers(final Address address, final String timeStamp) {

		// TODO: probably it'll be better to a) ask accountDao about account
		// TODO: b) pass obtained db-account to getTransactionsForAccount

		final Account account = this.accountCache.findByAddress(address);
		final Integer intTimeStamp = this.intOrMaxInt(timeStamp);
		final Collection<TransferBlockPair> pairs = this.transferDao.getTransactionsForAccount(account, intTimeStamp, DEFAULT_LIMIT);

		final SerializableList<TransactionMetaDataPair> transactionList = new SerializableList<>(0);
		pairs.stream()
				.map(pair -> new TransactionMetaDataPair(
						TransferMapper.toModel(pair.getTransfer(), this.accountCache),
						new TransactionMetaData(new BlockHeight(pair.getBlock().getHeight()), pair.getTransfer().getId())
				))
				.forEach(transactionList::add);
		return transactionList;
	}

	// TODO 20141201 J-B i guess a test for the new function?

	@Override
	public SerializableList<TransactionMetaDataPair> getAccountTransfersUsingHash(
			final Address address,
			final Hash transactionHash,
			final BlockHeight height,
			final ReadOnlyTransferDao.TransferType transfersType) {
		final Account account = this.accountCache.findByAddress(address);
		final Collection<TransferBlockPair> pairs = this.transferDao.getTransactionsForAccountUsingHash(account,
				transactionHash,
				height,
				transfersType,
				DEFAULT_LIMIT);

		final SerializableList<TransactionMetaDataPair> transactionList = new SerializableList<>(0);
		pairs.stream()
				.map(pair -> new TransactionMetaDataPair(
						TransferMapper.toModel(pair.getTransfer(), this.accountCache),
						new TransactionMetaData(new BlockHeight(pair.getBlock().getHeight()), pair.getTransfer().getId())
				))
				.forEach(transactionList::add);
		return transactionList;
	}

	@Override
	public SerializableList<TransactionMetaDataPair> getAccountTransfersUsingId(
			final Address address,
			final Long transactionId,
			final ReadOnlyTransferDao.TransferType transfersType) {
		final Account account = this.accountCache.findByAddress(address);
		final Collection<TransferBlockPair> pairs = this.transferDao.getTransactionsForAccountUsingId(account, transactionId, transfersType, DEFAULT_LIMIT);

		// TODO 20141201 J-B: this same mapping is happening in the preceding function, consider refactoring
		final SerializableList<TransactionMetaDataPair> transactionList = new SerializableList<>(0);
		pairs.stream()
				.map(pair -> new TransactionMetaDataPair(
						TransferMapper.toModel(pair.getTransfer(), this.accountCache),
						new TransactionMetaData(new BlockHeight(pair.getBlock().getHeight()), pair.getTransfer().getId())
				))
				.forEach(transactionList::add);
		return transactionList;
	}

	@Override
	public SerializableList<HarvestInfo> getAccountHarvests(final Address address, final Hash harvestHash) {
		final Account account = this.accountCache.findByAddress(address);
		final Collection<org.nem.nis.dbmodel.Block> blocks = this.blockDao.getBlocksForAccount(account, harvestHash, DEFAULT_LIMIT);

		final SerializableList<HarvestInfo> blockList = new SerializableList<>(0);

		blocks.stream()
				.map(bl -> new HarvestInfo(bl.getBlockHash(),
						new BlockHeight(bl.getHeight()),
						new TimeInstant(bl.getTimeStamp()),
						Amount.fromMicroNem(bl.getTotalFee())))
				.forEach(blockList::add);
		return blockList;
	}

	@Override
	public Iterator<Account> iterator() {
		return this.accountCache.iterator();
	}
}
