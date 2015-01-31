package org.nem.nis.mappers;

import org.nem.core.serialization.AccountLookup;

/**
 * A facade on top of the MapperFactory for creating NIS mapper facades.
 */
public class NisMapperFactory {
	private final MapperFactory mapperFactory;

	/**
	 * Creates a new nis mapper factory.
	 *
	 * @param factory The mapper factory.
	 */
	public NisMapperFactory(final MapperFactory factory) {
		this.mapperFactory = factory;
	}

	/**
	 * Creates a NIS mapper facade for mapping db model types to model types.
	 *
	 * @param accountLookup The account lookup.
	 * @return The mapper.
	 */
	public NisDbModelToModelMapper createDbModelToModelNisMapper(final AccountLookup accountLookup) {
		return new NisDbModelToModelMapper(this.mapperFactory.createDbModelToModelMapper(accountLookup));
	}
}
