package org.nem.nis.test;

import org.nem.core.model.Block;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

/**
 * Static class containing helper functions for mapper related tests.
 */
public class MapperUtils {

	// region create mapper factories

	/**
	 * Creates a NIS mapper factory.
	 *
	 * @return The NIS mapper factory.
	 */
	public static NisMapperFactory createNisMapperFactory() {
		return new NisMapperFactory(createMapperFactory());
	}

	// endregion

	// region create mappers

	/**
	 * Creates a NIS mapper facade for mapping model types to db model types.
	 *
	 * @param accountDao The account dao.
	 * @return The mapper.
	 */
	public static NisModelToDbModelMapper createModelToDbModelNisMapperAccountDao(final AccountDao accountDao) {
		return createModelToDbModelNisMapper(new AccountDaoLookupAdapter(accountDao));
	}

	private static NisModelToDbModelMapper createModelToDbModelNisMapper(final AccountDaoLookup accountDaoLookup) {
		return new NisModelToDbModelMapper(createMapperFactory().createModelToDbModelMapper(accountDaoLookup));
	}

	/**
	 * Creates a NIS mapper facade for mapping db model types to model types.
	 *
	 * @param accountLookup The account lookup.
	 * @return The mapper.
	 */
	public static NisDbModelToModelMapper createDbModelToModelNisMapper(final AccountLookup accountLookup) {
		return createNisMapperFactory().createDbModelToModelNisMapper(accountLookup);
	}

	// endregion

	// region mapping functions

	/**
	 * Maps a model block to a db model block.
	 *
	 * @param block The model block.
	 * @param accountDaoLookup The account dao lookup.
	 * @return The db model block.
	 */
	public static DbBlock toDbModel(final Block block, final AccountDaoLookup accountDaoLookup) {
		return MapperUtils.createModelToDbModelNisMapper(accountDaoLookup).map(block);
	}

	/**
	 * Maps a model block to a db model block.
	 *
	 * @param block The model block.
	 * @param accountDaoLookup The account dao lookup.
	 * @param mosaicIdCache The mosaic id cache.
	 * @return The db model block.
	 */
	public static DbBlock toDbModel(final Block block, final AccountDaoLookup accountDaoLookup, final MosaicIdCache mosaicIdCache) {
		final DefaultMapperFactory factory = new DefaultMapperFactory(mosaicIdCache);
		final NisModelToDbModelMapper mapper = new NisModelToDbModelMapper(factory.createModelToDbModelMapper(accountDaoLookup));
		return mapper.map(block);
	}

	/**
	 * Maps a model block to a db model block.
	 *
	 * @param block The model block.
	 * @param accountDaoLookup The account dao lookup.
	 * @return The db model block.
	 */
	public static DbBlock toDbModelWithHack(final Block block, final AccountDaoLookup accountDaoLookup) {
		// - hack: the problem is that the tests do something which cannot happen in a real environment
		// A mosaic supply change transaction is included in a block prior to the mosaic being in the db.
		// To overcome the problem, one MosaicId <--> DbMosaicId mapping is inserted into the mosaic id cache.
		final MosaicIdCache mosaicIdCache = new DefaultMosaicIdCache();
		mosaicIdCache.add(Utils.createMosaicDefinition(Utils.generateRandomAccount()).getId(), new DbMosaicId(1L));

		// - map the block
		return toDbModel(block, accountDaoLookup, mosaicIdCache);
	}

	/**
	 * Maps a db model block to a model block.
	 *
	 * @param dbBlock The db model block.
	 * @param accountLookup The account dao lookup.
	 * @return The model block.
	 */
	public static Block toModel(final DbBlock dbBlock, final AccountLookup accountLookup) {
		return MapperUtils.createDbModelToModelNisMapper(accountLookup).map(dbBlock);
	}

	/**
	 * Maps a db model block to a model block.
	 *
	 * @param dbBlock The db model block.
	 * @param accountLookup The account dao lookup.
	 * @param mosaicIdCache The mosaic id cache.
	 * @return The model block.
	 */
	public static Block toModel(final DbBlock dbBlock, final AccountLookup accountLookup, final MosaicIdCache mosaicIdCache) {
		final DefaultMapperFactory factory = new DefaultMapperFactory(mosaicIdCache);
		final NisDbModelToModelMapper mapper = new NisDbModelToModelMapper(factory.createDbModelToModelMapper(accountLookup));
		return mapper.map(dbBlock);
	}

	// endregion

	/**
	 * Creates a default mapper factory.
	 *
	 * @return The default mapper factory.
	 */
	public static DefaultMapperFactory createMapperFactory() {
		return new DefaultMapperFactory(new DefaultMosaicIdCache());
	}
}
