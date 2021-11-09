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
	private final ReadOnlyTransferDao transferDao;
	private final ReadOnlyBlockDao blockDao;
	private final ReadOnlyAccountCache accountCache;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	public AccountIoAdapter(final ReadOnlyTransferDao transferDao, final ReadOnlyBlockDao blockDao, final ReadOnlyAccountCache accountCache,
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

	@Override
	public SerializableList<TransactionMetaDataPair> getAccountTransfersUsingHash(final Address address, final Hash transactionHash,
			final BlockHeight height, final ReadOnlyTransferDao.TransferType transfersType, final int limit) {
		final Account account = this.accountCache.findByAddress(address);
		final Collection<TransferBlockPair> pairs = this.transferDao.getTransactionsForAccountUsingHash(account, transactionHash, height,
				transfersType, limit);
		return this.toSerializableTransactionMetaDataPairList(pairs);
	}

	@Override
	public SerializableList<TransactionMetaDataPair> getAccountTransfersUsingId(final Address address, final Long transactionId,
			final ReadOnlyTransferDao.TransferType transfersType, final int limit) {
		final Account account = this.accountCache.findByAddress(address);
		final Collection<TransferBlockPair> pairs = this.transferDao.getTransactionsForAccountUsingId(account, transactionId, transfersType,
				limit);
		return this.toSerializableTransactionMetaDataPairList(pairs);
	}

	private SerializableList<TransactionMetaDataPair> toSerializableTransactionMetaDataPairList(final Collection<TransferBlockPair> pairs) {
		final SerializableList<TransactionMetaDataPair> transactionList = new SerializableList<>(0);
		pairs.stream().map(pair -> {
			final Transaction transaction = this.mapper.map(pair.getTransfer());
			return new TransactionMetaDataPair(transaction,
					new TransactionMetaData(new BlockHeight(pair.getDbBlock().getHeight()), pair.getTransfer().getId(),
							pair.getTransfer().getTransferHash(),
							// TODO 20151124 J-G: should update tests
							transaction.getType() == TransactionTypes.MULTISIG
									? ((MultisigTransaction) transaction).getOtherTransactionHash()
									: null));
		}).forEach(transactionList::add);
		return transactionList;
	}

	@Override
	public SerializableList<HarvestInfo> getAccountHarvests(final Address address, final Long blockId, final int limit) {
		final Account account = this.accountCache.findByAddress(address);
		final Collection<DbBlock> blocks = this.blockDao.getBlocksForAccount(account, blockId, limit);

		final SerializableList<HarvestInfo> blockList = new SerializableList<>(0);

		blocks.stream().map(bl -> new HarvestInfo(bl.getId(), new BlockHeight(bl.getHeight()), new TimeInstant(bl.getTimeStamp()),
				Amount.fromMicroNem(bl.getTotalFee()), bl.getDifficulty())).forEach(blockList::add);
		return blockList;
	}
}
