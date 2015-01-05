package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dbmodel.MultisigSignature;

/**
 * Factory for creating a mapper.
 */
public class DefaultMapperFactory implements MapperFactory {

	@Override
	public MappingRepository createModelToDbModelMapper(final AccountDaoLookup accountDaoLookup) {
		final MappingRepository mappingRepository = new MappingRepository();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			entry.addModelToDbModelMappers(mappingRepository);
		}

		mappingRepository.addMapping(Account.class, org.nem.nis.dbmodel.Account.class, new AccountModelToDbModelMapping(accountDaoLookup));
		mappingRepository.addMapping(Block.class, org.nem.nis.dbmodel.Block.class, new BlockModelToDbModelMapping(mappingRepository));
		mappingRepository.addMapping(MultisigSignatureTransaction.class, MultisigSignature.class, new MultisigSignatureModelToDbModelMapping(mappingRepository));
		return mappingRepository;
	}

	@Override
	public MappingRepository createDbModelToModelMapper(final AccountLookup accountLookup) {
		final MappingRepository mappingRepository = new MappingRepository();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			entry.addDbModelToModelMappers(mappingRepository);
		}

		mappingRepository.addMapping(org.nem.nis.dbmodel.Account.class, Account.class, new AccountDbModelToModelMapping(accountLookup));
		mappingRepository.addMapping(org.nem.nis.dbmodel.Block.class, Block.class, new BlockDbModelToModelMapping(mappingRepository, accountLookup));
		mappingRepository.addMapping(MultisigSignature.class, MultisigSignatureTransaction.class, new MultisigSignatureDbModelToModelMapping(mappingRepository));
		return mappingRepository;
	}
}
