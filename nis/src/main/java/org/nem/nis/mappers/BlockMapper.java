package org.nem.nis.mappers;

import org.nem.core.model.Block;
import org.nem.core.serialization.*;

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
		return MapperFactory.createModelToDbModelMapper(accountDaoLookup).map(block, org.nem.nis.dbmodel.Block.class);
	}

	/**
	 * Converts a Block db-model to a Block model.
	 *
	 * @param dbBlock The block db-model.
	 * @param accountLookup The account lookup object.
	 * @return The Block model.
	 */
	public static Block toModel(final org.nem.nis.dbmodel.Block dbBlock, final AccountLookup accountLookup) {
		return MapperFactory.createDbModelToModelMapper(accountLookup).map(dbBlock, Block.class);
	}
}