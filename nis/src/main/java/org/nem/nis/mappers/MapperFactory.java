package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.serialization.AccountLookup;

/**
 * Factory for creating a mapper.
 */
public class MapperFactory {

	/**
	 * Creates a mapper that can map model types to db model types.
	 *
	 * @param accountDaoLookup The account dao lookup object.
	 * @return The mapper.
	 */
	public IMapper createModelToDbModelMapper(final AccountDaoLookup accountDaoLookup) {
		final MappingRepository mappingRepository = new MappingRepository();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			entry.addModelToDbModelMappers(mappingRepository);
		}

		mappingRepository.addMapping(Account.class, org.nem.nis.dbmodel.Account.class, new AccountModelToDbModelMapping(accountDaoLookup));
		mappingRepository.addMapping(Block.class, org.nem.nis.dbmodel.Block.class, new BlockModelToDbModelMapping(mappingRepository));
		return mappingRepository;
	}

	/**
	 * Creates a mapper that can map db model types to model types.
	 *
	 * @param accountLookup The account lookup object.
	 * @return The mapper.
	 */
	public IMapper createDbModelToModelMapper(final AccountLookup accountLookup) {
		final MappingRepository mappingRepository = new MappingRepository();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			entry.addDbModelToModelMappers(mappingRepository);
		}

		mappingRepository.addMapping(org.nem.nis.dbmodel.Account.class, Account.class, new AccountDbModelToModelMapping(accountLookup));
		mappingRepository.addMapping(org.nem.nis.dbmodel.Block.class, Block.class, new BlockDbModelToModelMapping(mappingRepository, accountLookup));
		return mappingRepository;
	}
}
