package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.test.*;

public class UnconfirmedTransactionMetaDataPairTest extends AbstractMetaDataPairTest<Transaction, UnconfirmedTransactionMetaData> {

	public UnconfirmedTransactionMetaDataPairTest() {
		super(
				address -> {
					final Transaction transfer = RandomTransactionFactory.createTransfer(new Account(address));
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
