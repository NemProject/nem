package org.nem.nis.test;

import org.nem.core.model.Block;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.cache.*;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.*;

/**
 * Static class containing helper functions for mapper related tests.
 */
public class MapperUtils {

	//region create mapper factories

	/**
	 * Creates a NIS mapper factory.
	 *
	 * @return The NIS mapper factory.
	 */
	public static NisMapperFactory createNisMapperFactory() {
		return new NisMapperFactory(createMapperFactory());
	}

	//endregion

	//region create mappers

	/**
	 * Creates a mapper for mapping model types to db model types.
	 *
	 * @param accountDao The account dao.
	 * @return The mapper.
	 */
	public static IMapper createModelToDbModelMapper(final AccountDao accountDao) {
		return createModelToDbModelMapper(new AccountDaoLookupAdapter(accountDao));
	}

	/**
	 * Creates a mapper for mapping model types to db model types.
	 *
	 * @param accountDaoLookup The account dao lookup.
	 * @return The mapper.
	 */
	public static IMapper createModelToDbModelMapper(final AccountDaoLookup accountDaoLookup) {
		return createMapperFactory().createModelToDbModelMapper(accountDaoLookup);
	}

	/**
	 * Creates a NIS mapper facade for mapping model types to db model types.
	 *
	 * @param accountDao The account dao.
	 * @return The mapper.
	 */
	public static NisModelToDbModelMapper createModelToDbModelNisMapper(final AccountDao accountDao) {
		return createModelToDbModelNisMapper(new AccountDaoLookupAdapter(accountDao));
	}

	/**
	 * Creates a NIS mapper facade for mapping model types to db model types.
	 *
	 * @param accountDaoLookup The account dao lookup.
	 * @return The mapper.
	 */
	public static NisModelToDbModelMapper createModelToDbModelNisMapper(final AccountDaoLookup accountDaoLookup) {
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

	//endregion

	//region mapping functions

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
	 * Maps a db model block to a model block.
	 *
	 * @param dbBlock The db model block.
	 * @param accountLookup The account dao lookup.
	 * @return The model block.
	 */
	public static Block toModel(final DbBlock dbBlock, final AccountLookup accountLookup) {
		return MapperUtils.createDbModelToModelNisMapper(accountLookup).map(dbBlock);
	}

	//endregion

	/**
	 * Creates a default mapper factory.
	 *
	 * @return The default mapper factory.
	 */
	public static DefaultMapperFactory createMapperFactory() {
		return new DefaultMapperFactory(new DefaultMosaicIdCache());
	}
}
