package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.namespace.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.dao.ReadOnlyTransferDao;

/**
 * An account input / output service.
 */
public interface AccountIo {

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
	 * @param transactionHash The hash of "top-most" transfer.
	 * @param height The block height at which to search for the hash.
	 * @param transfersType The type of transfers.
	 * @return The transaction information.
	 */
	SerializableList<TransactionMetaDataPair> getAccountTransfersUsingHash(
			final Address address,
			final Hash transactionHash,
			final BlockHeight height,
			final ReadOnlyTransferDao.TransferType transfersType);

	/**
	 * Gets all transaction information associated with an account.
	 *
	 * @param address The account address.
	 * @param transactionId The id of "top-most" transfer.
	 * @param transfersType The type of transfers.
	 * @return The transaction information.
	 */
	SerializableList<TransactionMetaDataPair> getAccountTransfersUsingId(
			final Address address,
			final Long transactionId,
			final ReadOnlyTransferDao.TransferType transfersType);

	/**
	 * Gets information about blocks harvested by an account.
	 *
	 * @param address The account address.
	 * @param id The id of "top-most" harvested block.
	 * @return The information about harvested blocks.
	 */
	SerializableList<HarvestInfo> getAccountHarvests(final Address address, final Long id);

	/**
	 * Gets information about namespaces owned by an account.
	 *
	 * @param address The account address.
	 * @param parent The parent namespace id (optional).
	 * @return The information about namespaces.
	 */
	SerializableList<Namespace> getAccountNamespaces(final Address address, final NamespaceId parent);

	/**
	 * Gets information about mosaic definitions owned by an account.
	 *
	 * @param address The account address.
	 * @param namespaceId The namespace id (optional).
	 * @return The information about mosaic definitions.
	 */
	SerializableList<MosaicDefinition> getAccountMosaicDefinitions(final Address address, final NamespaceId namespaceId, final Long id);
}
