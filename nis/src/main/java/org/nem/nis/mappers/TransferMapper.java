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
		// TODO 20141221: there's no need to recreate the MappingRepository each time
		final MappingRepository mappingRepository = new MappingRepository();
		mappingRepository.addMapping(TransferTransaction.class, Transfer.class, new TransferModelToDbModelMapping(mappingRepository));
		mappingRepository.addMapping(Account.class, org.nem.nis.dbmodel.Account.class, new AccountModelToDbModelMapping(accountDaoLookup));

		final Transfer dbTransfer = mappingRepository.map(transfer, Transfer.class);
		dbTransfer.setOrderId(orderIndex);
		dbTransfer.setBlkIndex(blockIndex);
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
		mappingRepository.addMapping(Transfer.class, TransferTransaction.class, new TransferDbModelToModelMapping(mappingRepository));
		mappingRepository.addMapping(org.nem.nis.dbmodel.Account.class, Account.class, new AccountDbModelToModelMapping(accountLookup));
		return mappingRepository.map(dbTransfer, TransferTransaction.class);
	}
}
