package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.cache.MosaicIdCache;
import org.nem.nis.controller.viewmodels.ExplorerBlockViewModel;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Factory for creating a mapper.
 */
public class DefaultMapperFactory implements MapperFactory {
	private final MosaicIdCache mosaicIdCache;

	private static class Entry<TDbModel, TModel> {
		public final Class<TDbModel> dbModelClass;
		public final Class<TModel> modelClass;

		private final BiFunction<AccountDaoLookup, IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper;
		private final BiFunction<AccountLookup, IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper;

		private Entry(final BiFunction<AccountDaoLookup, IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper,
				final BiFunction<AccountLookup, IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper,
				final Class<TDbModel> dbModelClass, final Class<TModel> modelClass) {
			this.createModelToDbModelMapper = createModelToDbModelMapper;
			this.createDbModelToModelMapper = createDbModelToModelMapper;
			this.dbModelClass = dbModelClass;
			this.modelClass = modelClass;
		}

		public void addModelToDbModelMappers(final AccountDaoLookup accountDaoLookup, final MappingRepository repository) {
			repository.addMapping(this.modelClass, this.dbModelClass, this.createModelToDbModelMapper.apply(accountDaoLookup, repository));
		}

		public void addDbModelToModelMappers(final AccountLookup accountLookup, final MappingRepository repository) {
			repository.addMapping(this.dbModelClass, this.modelClass, this.createDbModelToModelMapper.apply(accountLookup, repository));
		}
	}

	@SuppressWarnings("serial")
	private static final List<Entry<?, ?>> ENTRIES = new ArrayList<Entry<?, ?>>() {
		{
			this.add(new Entry<>((lookup, mapper) -> new AccountModelToDbModelMapping(lookup),
					(lookup, mapper) -> new AccountDbModelToModelMapping(lookup), DbAccount.class, Account.class));
			this.add(new Entry<>((lookup, mapper) -> new BlockModelToDbModelMapping(mapper),
					(lookup, mapper) -> new BlockDbModelToModelMapping(mapper), DbBlock.class, Block.class));
			this.add(new Entry<>((lookup, mapper) -> new MultisigSignatureModelToDbModelMapping(mapper),
					(lookup, mapper) -> new MultisigSignatureDbModelToModelMapping(mapper), DbMultisigSignatureTransaction.class,
					MultisigSignatureTransaction.class));
			this.add(new Entry<>((lookup, mapper) -> new NamespaceModelToDbModelMapping(mapper),
					(lookup, mapper) -> new NamespaceDbModelToModelMapping(mapper), DbNamespace.class, Namespace.class));
			this.add(new Entry<>((lookup, mapper) -> new MosaicDefinitionModelToDbModelMapping(mapper),
					(lookup, mapper) -> new MosaicDefinitionDbModelToModelMapping(mapper), DbMosaicDefinition.class,
					MosaicDefinition.class));
			this.add(new Entry<>((lookup, mapper) -> new MosaicPropertyModelToDbModelMapping(),
					(lookup, mapper) -> new MosaicPropertyDbModelToModelMapping(), DbMosaicProperty.class, NemProperty.class));
			this.add(new Entry<>((lookup, mapper) -> new MosaicModelToDbModelMapping(mapper),
					(lookup, mapper) -> new MosaicDbModelToModelMapping(mapper), DbMosaic.class, Mosaic.class));
		}
	};

	/**
	 * Creates a new default mapper factory.
	 *
	 * @param mosaicIdCache The mosaic id cache.
	 */
	public DefaultMapperFactory(final MosaicIdCache mosaicIdCache) {
		this.mosaicIdCache = mosaicIdCache;
	}

	/**
	 * Creates a model to db model mapper.
	 *
	 * @param accountDaoLookup The account dao lookup.
	 * @return The mapping repository.
	 */
	public MappingRepository createModelToDbModelMapper(final AccountDaoLookup accountDaoLookup) {
		final MappingRepository mappingRepository = new MappingRepository();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			entry.addModelToDbModelMappers(mappingRepository);
		}

		for (final Entry<?, ?> entry : ENTRIES) {
			entry.addModelToDbModelMappers(accountDaoLookup, mappingRepository);
		}

		mappingRepository.addMapping(MosaicId.class, DbMosaicId.class, new MosaicIdModelToDbModelMapping(this.mosaicIdCache));
		return mappingRepository;
	}

	@Override
	public MappingRepository createDbModelToModelMapper(final AccountLookup accountLookup) {
		final MappingRepository mappingRepository = new MappingRepository();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			entry.addDbModelToModelMappers(mappingRepository);
		}

		for (final Entry<?, ?> entry : ENTRIES) {
			entry.addDbModelToModelMappers(accountLookup, mappingRepository);
		}

		mappingRepository.addMapping(DbMosaicId.class, MosaicId.class, new MosaicIdDbModelToModelMapping(this.mosaicIdCache));
		mappingRepository.addMapping(DbBlock.class, ExplorerBlockViewModel.class,
				new BlockDbModelToExplorerViewModelMapping(mappingRepository));
		return mappingRepository;
	}
}
