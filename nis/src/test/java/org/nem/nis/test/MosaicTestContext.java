package org.nem.nis.test;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.dao.ReadOnlyNamespaceDao;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.MosaicInfoFactory;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.Collectors;

public class MosaicTestContext {
	private final Map<Address, AccountState> stateMap = new HashMap<>();
	protected final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
	protected final NamespaceCache namespaceCache = Mockito.mock(NamespaceCache.class);
	protected final MosaicInfoFactory mosaicInfoFactory = new MosaicInfoFactory(this.accountStateCache, this.namespaceCache,
			Mockito.mock(ReadOnlyNamespaceDao.class), Mockito.mock(NisDbModelToModelMapper.class), () -> new BlockHeight(123));
	private final HashMap<MosaicId, MosaicDefinition> mosaicDefinitions = new HashMap<>();

	public MosaicId createMosaicId(final String namespaceName, final String mosaicName) {
		return this.createMosaicId(namespaceName, mosaicName, 0L);
	}

	public MosaicId createMosaicId(final int id, final Long initialSupply) {
		return this.createMosaicId(String.format("id%d", id), String.format("name%d", id), initialSupply);
	}

	private MosaicId createMosaicId(final String namespaceName, final String mosaicName, final Long initialSupply) {
		return this.createMosaicId(namespaceName, mosaicName, initialSupply, null);
	}

	protected MosaicId createMosaicId(final String namespaceName, final String mosaicName, final Long initialSupply,
			final MosaicLevy levy) {
		final MosaicId mosaicId = Utils.createMosaicId(namespaceName, mosaicName);
		final MosaicDefinition mosaicDefinition = new MosaicDefinition(Utils.generateRandomAccount(), mosaicId,
				new MosaicDescriptor("descriptor"), Utils.createMosaicPropertiesWithInitialSupply(initialSupply), levy);
		this.mosaicDefinitions.put(mosaicId, mosaicDefinition);
		return mosaicId;
	}

	public void addXemMosaic() {
		this.mosaicDefinitions.put(MosaicConstants.MOSAIC_ID_XEM, MosaicConstants.MOSAIC_DEFINITION_XEM);
	}

	public void prepareMosaics(final List<MosaicId> mosaicIds) {
		final Set<NamespaceId> uniqueNamespaces = mosaicIds.stream().map(MosaicId::getNamespaceId).collect(Collectors.toSet());
		for (final NamespaceId namespaceId : uniqueNamespaces) {
			final NamespaceEntry namespaceEntry = Mockito.mock(NamespaceEntry.class);
			Mockito.when(this.namespaceCache.get(namespaceId)).thenReturn(namespaceEntry);

			final Mosaics mosaics = Mockito.mock(Mosaics.class);
			Mockito.when(namespaceEntry.getMosaics()).thenReturn(mosaics);
		}

		for (final MosaicId mosaicId : mosaicIds) {
			final ReadOnlyMosaics mosaics = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics();

			final MosaicDefinition mosaicDefinition = this.mosaicDefinitions.get(mosaicId);
			final MosaicEntry mosaicEntry = new MosaicEntry(mosaicDefinition);
			Mockito.when(mosaics.get(mosaicId)).thenReturn(mosaicEntry);
		}
	}

	public void ownsMosaic(final Address address, final List<MosaicId> mosaicIds) {
		final AccountState accountState = new AccountState(address);
		mosaicIds.forEach(id -> accountState.getAccountInfo().addMosaicId(id));
		Mockito.when(this.accountStateCache.findStateByAddress(address)).thenReturn(accountState);
		this.stateMap.put(address, accountState);
	}

	public void setBalance(final MosaicId mosaicId, final Address address, final Quantity balance) {
		final MosaicEntry mosaicEntry = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		mosaicEntry.getBalances().incrementBalance(address, balance);
	}

	public void incrementXemBalance(final Address address, final Amount balance) {
		// use a state map here so that the mocked stateMap counts aren't affected
		this.stateMap.get(address).getAccountInfo().incrementBalance(balance);
	}

	public void assertMosaicDefinitionsOwned(final Collection<MosaicDefinition> returnedMosaicDefinitions, final List<MosaicId> expected) {
		MatcherAssert.assertThat(returnedMosaicDefinitions.size(), IsEqual.equalTo(expected.size()));

		final Set<MosaicDefinition> definitions = expected.stream().map(this.mosaicDefinitions::get).collect(Collectors.toSet());
		MatcherAssert.assertThat(returnedMosaicDefinitions, IsEquivalent.equivalentTo(definitions));
	}

	public void assertMosaicsOwned(final Collection<Mosaic> returnedMosaics, final List<Mosaic> expectedMosaics) {
		MatcherAssert.assertThat(returnedMosaics.size(), IsEqual.equalTo(expectedMosaics.size()));
		MatcherAssert.assertThat(returnedMosaics, IsEquivalent.equivalentTo(expectedMosaics));
	}
}
