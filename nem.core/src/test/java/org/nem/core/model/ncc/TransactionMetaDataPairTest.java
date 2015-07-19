package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

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
