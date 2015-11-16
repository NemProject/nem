package org.nem.nis.service;

import org.nem.core.model.Address;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.mosaic.MosaicConstants;
import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.cache.ReadOnlyNamespaceCache;
import org.nem.nis.state.ReadOnlyAccountState;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

	public List<Mosaic> getAccountOwnedMosaics(Address address) {
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		return accountState.getAccountInfo().getMosaicIds().stream()
				.map(mosaicId -> this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId))
				.map(entry -> new Mosaic(
						entry.getMosaicDefinition().getId(),
						entry.getBalances().getBalance(accountState.getAddress())))
				.collect(Collectors.toList());
	}
}
