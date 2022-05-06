package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.model.primitive.BlockHeight;

public interface TransactionIo {

	/**
	 * Requests information about the transaction the specified hash.
	 *
	 * @param blockHeight The height of the block in which the transaction is stored.
	 * @param hash The transaction hash.
	 * @return The transaction with the specified hash associated with its meta data.
	 */
	TransactionMetaDataPair getTransactionUsingHash(Hash hash, BlockHeight blockHeight);
}
