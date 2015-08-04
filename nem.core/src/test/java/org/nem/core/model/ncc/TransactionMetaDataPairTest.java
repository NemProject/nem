package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Transaction;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.RandomTransactionFactory;

public class TransactionMetaDataPairTest extends AbstractMetaDataPairTest<Transaction, TransactionMetaData> {

	public TransactionMetaDataPairTest() {
		super(
				account -> {
					final Transaction transfer = RandomTransactionFactory.createTransfer(account);
					transfer.sign();
					return transfer;
				},
				id -> new TransactionMetaData(BlockHeight.ONE, (long)id, Hash.ZERO),
				TransactionMetaDataPair::new,
				TransactionMetaDataPair::new,
				transaction -> transaction.getSigner().getAddress(),
				metaData -> metaData.getId().intValue());
	}
}
