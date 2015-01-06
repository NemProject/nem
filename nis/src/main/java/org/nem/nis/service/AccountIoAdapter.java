package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.ReadOnlyAccountCache;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class AccountIoAdapter implements AccountIo {
	private static final int DEFAULT_LIMIT = 25;

	private final ReadOnlyTransferDao transferDao;
	private final ReadOnlyBlockDao blockDao;
	private final ReadOnlyAccountCache accountCache;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	public AccountIoAdapter(
			final ReadOnlyTransferDao transferDao,
			final ReadOnlyBlockDao blockDao,
			final ReadOnlyAccountCache accountCache,
			final NisDbModelToModelMapper mapper) {
		this.transferDao = transferDao;
		this.blockDao = blockDao;
		this.accountCache = accountCache;
		this.mapper = mapper;
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

	@Override
	public SerializableList<TransactionMetaDataPair> getAccountTransfers(final Address address, final String timeStamp) {
		final Account account = this.accountCache.findByAddress(address);
		final Integer intTimeStamp = this.intOrMaxInt(timeStamp);
		final Collection<TransferBlockPair> pairs = this.transferDao.getTransactionsForAccount(account, intTimeStamp, DEFAULT_LIMIT);
		return this.toSerializableTransactionMetaDataPairList(pairs);
	}

	@Override
	public SerializableList<TransactionMetaDataPair> getAccountTransfersUsingHash(
			final Address address,
			final Hash transactionHash,
			final BlockHeight height,
			final ReadOnlyTransferDao.TransferType transfersType) {
		final Account account = this.accountCache.findByAddress(address);
		final Collection<TransferBlockPair> pairs = this.transferDao.getTransactionsForAccountUsingHash(
				account,
				transactionHash,
				height,
				transfersType,
				DEFAULT_LIMIT);
		return this.toSerializableTransactionMetaDataPairList(pairs);
	}

	@Override
	public SerializableList<TransactionMetaDataPair> getAccountTransfersUsingId(
			final Address address,
			final Long transactionId,
			final ReadOnlyTransferDao.TransferType transfersType) {
		final Account account = this.accountCache.findByAddress(address);
		final Collection<TransferBlockPair> pairs = this.transferDao.getTransactionsForAccountUsingId(account, transactionId, transfersType, DEFAULT_LIMIT);
		return this.toSerializableTransactionMetaDataPairList(pairs);
	}

	private SerializableList<TransactionMetaDataPair> toSerializableTransactionMetaDataPairList(final Collection<TransferBlockPair> pairs) {
		final SerializableList<TransactionMetaDataPair> transactionList = new SerializableList<>(0);
		pairs.stream()
				.map(pair -> new TransactionMetaDataPair(
						this.mapper.map(pair.getDbTransferTransaction()),
						new TransactionMetaData(new BlockHeight(pair.getDbBlock().getHeight()), pair.getDbTransferTransaction().getId())
				))
				.forEach(transactionList::add);
		return transactionList;
	}

	@Override
	public SerializableList<HarvestInfo> getAccountHarvests(final Address address, final Hash harvestHash) {
		final Account account = this.accountCache.findByAddress(address);
		final Collection<DbBlock> blocks = this.blockDao.getBlocksForAccount(account, harvestHash, DEFAULT_LIMIT);

		final SerializableList<HarvestInfo> blockList = new SerializableList<>(0);

		blocks.stream()
				.map(bl -> new HarvestInfo(bl.getBlockHash(),
						new BlockHeight(bl.getHeight()),
						new TimeInstant(bl.getTimeStamp()),
						Amount.fromMicroNem(bl.getTotalFee())))
				.forEach(blockList::add);
		return blockList;
	}
}
