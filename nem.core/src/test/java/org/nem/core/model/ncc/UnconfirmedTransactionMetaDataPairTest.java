package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Transaction;
import org.nem.core.test.RandomTransactionFactory;

public class UnconfirmedTransactionMetaDataPairTest extends AbstractMetaDataPairTest<Transaction, UnconfirmedTransactionMetaData> {

	public UnconfirmedTransactionMetaDataPairTest() {
		super(
				account -> {
					final Transaction transfer = RandomTransactionFactory.createTransfer(account);
					transfer.sign();
					return transfer;
				},
				id -> new UnconfirmedTransactionMetaData(Hash.fromHexString(Integer.toHexString(id))),
				UnconfirmedTransactionMetaDataPair::new,
				UnconfirmedTransactionMetaDataPair::new,
				transaction -> transaction.getSigner().getAddress(),
				metaData -> Integer.parseInt(metaData.getInnerTransactionHash().toString(), 16));
	}
}
