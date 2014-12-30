package org.nem.nis.mappers;

import org.nem.core.model.Block;
import org.nem.core.serialization.*;

/**
 * Static class that contains functions for converting to and from
 * db-model Block and model Block.
 // TODO 20141230 J-J: remove this class! temporary class!
 */
public class BlockMapper {

	/**
	 * Converts a Block db-model to a Block model.
	 *
	 * @param dbBlock The block db-model.
	 * @param accountLookup The account lookup object.
	 * @return The Block model.
	 */
	public static Block toModel(final org.nem.nis.dbmodel.Block dbBlock, final AccountLookup accountLookup) {
		return new MapperFactory().createDbModelToModelMapper(accountLookup).map(dbBlock, Block.class);
	}
}