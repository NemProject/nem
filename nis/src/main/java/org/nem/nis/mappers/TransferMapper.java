package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dbmodel.*;

/**
 * Static class that contains functions for converting to and from
 * db-model Transfer and model TransferTransaction.
 */
public class TransferMapper {

	/**
	 * Converts a TransferTransaction model to a Transfer db-model.
	 *
	 * @param transfer The transfer transaction model.
	 * @param blockIndex The index of the transfer within the owning block.
	 * @param orderIndex The index of the transfer within the owning block's collection of similar transactions.
	 * @param accountDaoLookup The account dao lookup object.
	 * @return The Transfer db-model.
	 */
	public static Transfer toDbModel(
			final TransferTransaction transfer,
			final int blockIndex,
			final int orderIndex,
			final AccountDaoLookup accountDaoLookup) {
		final org.nem.nis.dbmodel.Account sender = accountDaoLookup.findByAddress(transfer.getSigner().getAddress());
		final org.nem.nis.dbmodel.Account recipient = accountDaoLookup.findByAddress(transfer.getRecipient().getAddress());

		final Transfer dbTransfer = new Transfer();
		AbstractTransferMapper.toDbModel(transfer, sender, blockIndex, orderIndex, dbTransfer);

		dbTransfer.setRecipient(recipient);
		dbTransfer.setAmount(transfer.getAmount().getNumMicroNem());

		final Message message = transfer.getMessage();
		if (null != message) {
			dbTransfer.setMessageType(message.getType());
			dbTransfer.setMessagePayload(message.getEncodedPayload());
		}

		return dbTransfer;
	}

	/**
	 * Converts a Transfer db-model to a TransferTransaction model.
	 *
	 * @param dbTransfer The transfer db-model.
	 * @param accountLookup The account lookup object.
	 * @return The TransferTransaction model.
	 */
	public static TransferTransaction toModel(final Transfer dbTransfer, final AccountLookup accountLookup) {
		// TODO 20141221: there's no need to recreate the MappingRepository each time
		final MappingRepository mappingRepository = new MappingRepository();
		mappingRepository.addMapping(Transfer.class, TransferTransaction.class, new TransferToTransactionMapping(mappingRepository));
		mappingRepository.addMapping(org.nem.nis.dbmodel.Account.class, Account.class, new DbAccountToAccountMapping(accountLookup));
		return mappingRepository.map(dbTransfer, TransferTransaction.class);
	}
}
