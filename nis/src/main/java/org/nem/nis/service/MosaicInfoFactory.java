package org.nem.nis.service;

import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.state.ReadOnlyAccountState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MosaicInfoFactory {
	private final ReadOnlyAccountStateCache accountStateCache;
	private final ReadOnlyNamespaceCache namespaceCache;

	@Autowired(required = true)
	public MosaicInfoFactory(
			final ReadOnlyAccountStateCache accountStateCache, ReadOnlyNamespaceCache namespaceCache) {
		this.accountStateCache = accountStateCache;
		this.namespaceCache = namespaceCache;
	}

	public Set<MosaicDefinition> getMosaicDefinitions(Address address) {
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

	private MosaicDefinition getMosaicDefinition(final MosaicId mosaicId) {
		return this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId).getMosaicDefinition();
	}

	private MosaicMetaData getMosaicMetaData(final MosaicId mosaicId) {
		return new MosaicMetaData(this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId).getSupply());
	}

	public List<Mosaic> getAccountOwnedMosaics(Address address) {
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		return Stream.concat(Stream.of(MosaicConstants.MOSAIC_ID_XEM), accountState.getAccountInfo().getMosaicIds().stream())
				.map(mosaicId -> this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId))
				.map(entry -> new Mosaic(
						entry.getMosaicDefinition().getId(),
						entry.getBalances().getBalance(accountState.getAddress())))
				.collect(Collectors.toList());
	}

	public Set<MosaicDefinitionMetaDataPair> getMosaicDefinitionsMetaDataPairs(final Address address) {
		final Set<MosaicDefinition> mosaicDefinitions = this.getMosaicDefinitions(address);

		// add owned mosaic definitions
		final Set<MosaicDefinitionMetaDataPair> results = mosaicDefinitions.stream()
				.map(definition -> new MosaicDefinitionMetaDataPair(definition, this.getMosaicMetaData(definition.getId())))
				.collect(Collectors.toSet());

		return results;
	}
}
