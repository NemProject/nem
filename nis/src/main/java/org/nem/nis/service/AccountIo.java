package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.dao.ReadOnlyTransferDao;

/**
 * An account input / output service.
 */
public interface AccountIo extends Iterable<Account> {

	/**
	 * Finds an account given an address.
	 *
	 * @param address The address.
	 * @return The account.
	 */
	Account findByAddress(final Address address);

	/**
	 * Gets all transaction information associated with an account.
	 *
	 * @param address The account address.
	 * @param timeStamp The maximum timestamp of a transaction.
	 * @return The transaction information.
	 */
	SerializableList<TransactionMetaDataPair> getAccountTransfers(final Address address, final String timeStamp);

	/**
	 * Gets all transaction information associated with an account.
	 *
	 * @param address The account address.
	 * @param transactionHash The hash of "top-most" transfer.
	 * @param transfersType The type of transfers.
	 * @return The transaction information.
	 */
	SerializableList<TransactionMetaDataPair> getAccountTransfersWithHash(final Address address, final Hash transactionHash, final ReadOnlyTransferDao.TransferType transfersType);
	/**
	 * Gets information about blocks harvested by an account.
	 *
	 * @param address The account address.
	 * @param timeStamp The maximum timestamp of a block.
	 * @return The information about harvested blocks.
	 */
	SerializableList<HarvestInfo> getAccountHarvests(final Address address, final String timeStamp);
}
