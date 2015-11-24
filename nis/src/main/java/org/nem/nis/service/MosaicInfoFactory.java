package org.nem.nis.service;

import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.dao.ReadOnlyNamespaceDao;
import org.nem.nis.dbmodel.DbNamespace;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.state.ReadOnlyAccountState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO 20151124 J-G: i like this refactoring, but tests ^^
@Service
public class MosaicInfoFactory {
	private final ReadOnlyAccountStateCache accountStateCache;
	private final ReadOnlyNamespaceCache namespaceCache;
	private final ReadOnlyNamespaceDao namespaceDao;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	public MosaicInfoFactory(
			final ReadOnlyAccountStateCache accountStateCache,
			final ReadOnlyNamespaceCache namespaceCache,
			final ReadOnlyNamespaceDao namespaceDao,
			final NisDbModelToModelMapper mapper) {
		this.accountStateCache = accountStateCache;
		this.namespaceCache = namespaceCache;
		this.namespaceDao = namespaceDao;
		this.mapper = mapper;
	}

	public Set<MosaicDefinition> getMosaicDefinitions(final Address address) {
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);

		// add owned mosaic definitions
		final Set<MosaicDefinition> mosaicDefinitions = accountState.getAccountInfo().getMosaicIds().stream()
				.map(this::getMosaicDefinition)
				.collect(Collectors.toSet());

		// add 1st level levies too
		final Set<MosaicDefinition> mosaicLevyDefinitions = mosaicDefinitions.stream()
				.filter(def -> null != def.getMosaicLevy())
				.map(def -> def.getMosaicLevy().getMosaicId())
				.map(this::getMosaicDefinition)
				.collect(Collectors.toSet());
		mosaicDefinitions.addAll(mosaicLevyDefinitions);

		// always add xem mosaic
		mosaicDefinitions.add(MosaicConstants.MOSAIC_DEFINITION_XEM);
		return mosaicDefinitions;
	}

	public MosaicDefinition getMosaicDefinition(final MosaicId mosaicId) {
		return this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId).getMosaicDefinition();
	}

	private Supply getMosaicSupply(final MosaicId mosaicId) {
		return this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId).getSupply();
	}

	public List<Mosaic> getAccountOwnedMosaics(final Address address) {
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		final Mosaic nemXemBalance = new Mosaic(MosaicConstants.MOSAIC_ID_XEM, Quantity.fromValue(accountState.getAccountInfo().getBalance().getNumMicroNem()));
		return Stream.concat(Stream.of(nemXemBalance),
				accountState.getAccountInfo().getMosaicIds().stream()
						.map(mosaicId -> this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId))
						.map(entry -> new Mosaic(
								entry.getMosaicDefinition().getId(),
								entry.getBalances().getBalance(accountState.getAddress())))
				)
				.collect(Collectors.toList());
	}

	public Set<MosaicDefinitionSupplyPair> getMosaicDefinitionsMetaDataPairs(final Address address) {
		final Set<MosaicDefinition> mosaicDefinitions = this.getMosaicDefinitions(address);

		// add owned mosaic definitions
		return mosaicDefinitions.stream()
				.map(definition -> new MosaicDefinitionSupplyPair(definition, this.getMosaicSupply(definition.getId())))
				.collect(Collectors.toSet());
	}

	public Set<Namespace> getAccountOwnedNamespaces(final Address address) {
		final Collection<DbNamespace> namespaces = this.namespaceDao.getNamespacesForAccount(
				address,
				null,
				1000);

		return namespaces.stream().map(dbNamespace -> this.mapper.map(dbNamespace, Namespace.class)).collect(Collectors.toSet());
	}
}
