package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.ImportanceTransfer;

/**
 * Static class that contains functions for converting to and from
 * db-model ImportanceTransfer and model ImportanceTransferTransaction.
 */
public class ImportanceTransferMapper {
	/**
	 * Converts a ImportanceTransferTransaction model to a ImportanceTransfer db-model.
	 * TODO 20141010 J-G: do we need both blockIndex and orderIndex?
	 * TODO 20131110 G-J: just to let you know, I remember about this, I'll try to address this in some future build,
	 * TODO  but I'm not sure if that will be doable.
	 *
	 * @param importanceTransferTransaction The transfer transaction model.
	 * @param blockIndex The index of the transfer within the owning block.
	 * @param orderIndex The index of the transfer within the owning block's collection of similar transactions.
	 * @param accountDaoLookup The account dao lookup object.
	 * @return The ImportanceTransfer db-model.
	 */
	public static ImportanceTransfer toDbModel(
			final ImportanceTransferTransaction importanceTransferTransaction,
			final int blockIndex,
			final int orderIndex,
			final AccountDaoLookup accountDaoLookup) {
		// TODO 20141221: there's no need to recreate the MappingRepository each time
		final MappingRepository mappingRepository = new MappingRepository();
		mappingRepository.addMapping(ImportanceTransferTransaction.class, ImportanceTransfer.class, new ImportanceTransferModelToDbModelMapping(mappingRepository));
		mappingRepository.addMapping(Account.class, org.nem.nis.dbmodel.Account.class, new AccountModelToDbModelMapping(accountDaoLookup));

		final ImportanceTransfer dbTransfer = mappingRepository.map(importanceTransferTransaction, ImportanceTransfer.class);
		dbTransfer.setOrderId(orderIndex);
		dbTransfer.setBlkIndex(blockIndex);
		return dbTransfer;
	}

	/**
	 * Converts a ImportanceTransfer db-model to a ImportanceTransferTransaction model.
	 *
	 * @param dbImportanceTransfer The importance transfer db-model.
	 * @param accountLookup The account lookup object.
	 * @return The ImportanceTransferTransaction model.
	 */
	public static ImportanceTransferTransaction toModel(final ImportanceTransfer dbImportanceTransfer, final AccountLookup accountLookup) {
		// TODO 20141221: there's no need to recreate the MappingRepository each time
		final MappingRepository mappingRepository = new MappingRepository();
		mappingRepository.addMapping(
				ImportanceTransfer.class,
				ImportanceTransferTransaction.class,
				new ImportanceTransferDbModelToModelMapping(mappingRepository));
		mappingRepository.addMapping(org.nem.nis.dbmodel.Account.class, Account.class, new AccountDbModelToModelMapping(accountLookup));
		return mappingRepository.map(dbImportanceTransfer, ImportanceTransferTransaction.class);
	}
}
