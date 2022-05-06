package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.dbmodel.TransferBlockPair;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.MissingResourceException;

@Service
public class DbTransferIoAdapter implements TransactionIo {
	private final ReadOnlyTransferDao transferDao;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	public DbTransferIoAdapter(final ReadOnlyTransferDao transferDao, final NisDbModelToModelMapper mapper) {
		this.transferDao = transferDao;
		this.mapper = mapper;
	}

	@Override
	public TransactionMetaDataPair getTransactionUsingHash(Hash hash, BlockHeight blockHeight) {
		final TransferBlockPair pair = this.transferDao.getTransactionUsingHash(hash, blockHeight);
		if (null == pair) {
			throw createMissingResourceException(hash.toString());
		}

		final Transaction transaction = this.mapper.map(pair.getTransfer());
		return new TransactionMetaDataPair(transaction, new TransactionMetaData(new BlockHeight(pair.getDbBlock().getHeight()),
				pair.getTransfer().getId(), pair.getTransfer().getTransferHash(),
				transaction.getType() == TransactionTypes.MULTISIG ? ((MultisigTransaction) transaction).getOtherTransactionHash() : null));
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("transaction not found in the db", Transaction.class.getName(), key);
	}
}
