package org.nem.nis.mappers;

import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.*;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * Static class that contains functions for converting to and from
 * db-model Block and model Block.
 */
public class BlockMapper {

	/**
	 * Converts a Block model to a Block db-model.
	 *
	 * @param block The block model.
	 * @param accountDaoLookup The account dao lookup object.
	 * @return The Block db-model.
	 */
	public static org.nem.nis.dbmodel.Block toDbModel(final Block block, final AccountDaoLookup accountDaoLookup) {
		// TODO 20141221: there's no need to recreate the MappingRepository each time
		final MappingRepository mappingRepository = new MappingRepository();
		mappingRepository.addMapping(TransferTransaction.class, Transfer.class, new TransferModelToDbModelMapping(mappingRepository));
		mappingRepository.addMapping(ImportanceTransferTransaction.class, ImportanceTransfer.class, new ImportanceTransferModelToDbModelMapping(mappingRepository));
		mappingRepository.addMapping(Account.class, org.nem.nis.dbmodel.Account.class, new AccountModelToDbModelMapping(accountDaoLookup));
		mappingRepository.addMapping(Block.class, org.nem.nis.dbmodel.Block.class, new BlockModelToDbModelMapping(mappingRepository));
		return mappingRepository.map(block, org.nem.nis.dbmodel.Block.class);
	}

	/**
	 * Converts a Block db-model to a Block model.
	 *
	 * @param dbBlock The block db-model.
	 * @param accountLookup The account lookup object.
	 * @return The Block model.
	 */
	public static Block toModel(final org.nem.nis.dbmodel.Block dbBlock, final AccountLookup accountLookup) {
		// TODO 20141221: there's no need to recreate the MappingRepository each time
		final MappingRepository mappingRepository = new MappingRepository();
		mappingRepository.addMapping(Transfer.class, TransferTransaction.class, new TransferDbModelToModelMapping(mappingRepository));
		mappingRepository.addMapping(ImportanceTransfer.class, ImportanceTransferTransaction.class, new ImportanceTransferDbModelToModelMapping(mappingRepository));
		mappingRepository.addMapping(org.nem.nis.dbmodel.Account.class, Account.class, new AccountDbModelToModelMapping(accountLookup));
		mappingRepository.addMapping(org.nem.nis.dbmodel.Block.class, Block.class, new BlockDbModelToModelMapping(mappingRepository, accountLookup));
		return mappingRepository.map(dbBlock, Block.class);
	}
}