package org.nem.nis.mappers;

import org.nem.core.serialization.AccountLookup;

/**
 * Factory for creating a mapper.
 */
public interface MapperFactory {

	/**
	 * Creates a mapper that can map model types to db model types.
	 *
	 * @param accountDaoLookup The account dao lookup object.
	 * @return The mapper.
	 */
	IMapper createModelToDbModelMapper(final AccountDaoLookup accountDaoLookup);

	/**
	 * Creates a mapper that can map db model types to model types.
	 *
	 * @param accountLookup The account lookup object.
	 * @return The mapper.
	 */
	IMapper createDbModelToModelMapper(final AccountLookup accountLookup);
}
