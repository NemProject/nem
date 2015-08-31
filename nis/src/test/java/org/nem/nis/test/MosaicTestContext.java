package org.nem.nis.test;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.AccountIdBuilder;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.controller.requests.MosaicIdBuilder;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.Collectors;

public class MosaicTestContext {
	public final Address address = Utils.generateRandomAddressWithPublicKey();

	public final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
	public final NamespaceCache namespaceCache = Mockito.mock(NamespaceCache.class);
	public final HashMap<MosaicId, MosaicDefinition> mosaicDefinitions = new HashMap<>();

	public AccountIdBuilder getAccountIdBuilder(final Address address) {
		final AccountIdBuilder builder = new AccountIdBuilder();
		builder.setAddress(address.getEncoded());
		return builder;
	}

	public MosaicIdBuilder getMosaicIdBuilder(final String mosaicId) {
		final MosaicIdBuilder builder = new MosaicIdBuilder();
		builder.setMosaicId(mosaicId);
		return builder;
	}

	public MosaicId createMosaicId(final String namespaceName, final String mosaicName) {
		return this.createMosaicId(namespaceName, mosaicName, 0L);
	}

	public MosaicId createMosaicId(final int id, final Long initialSupply) {
		return this.createMosaicId(String.format("id%d", id), String.format("name%d", id), initialSupply);
	}

	public MosaicId createMosaicId(final String namespaceName, final String mosaicName, final Long initialSupply) {
		final MosaicId mosaicId = Utils.createMosaicId(namespaceName, mosaicName);
		final MosaicDefinition mosaicDefinition = new MosaicDefinition(
				Utils.generateRandomAccount(),
				mosaicId,
				new MosaicDescriptor("descriptor"),
				Utils.createMosaicProperties(initialSupply),
				null);
		this.mosaicDefinitions.put(mosaicId, mosaicDefinition);
		return mosaicId;
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
	}

	public void setBalance(final MosaicId mosaicId, final Address address, final Quantity balance) {
		final MosaicEntry mosaicEntry = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		mosaicEntry.getBalances().incrementBalance(address, balance);
	}

	public void assertMosaicDefinitionsOwned(final Collection<MosaicDefinition> returnedMosaicDefinitions, final List<MosaicId> expected) {
		Assert.assertThat(returnedMosaicDefinitions.size(), IsEqual.equalTo(expected.size()));

		final Set<MosaicDefinition> definitions = expected.stream().map(this.mosaicDefinitions::get).collect(Collectors.toSet());
		Assert.assertThat(returnedMosaicDefinitions, IsEquivalent.equivalentTo(definitions));
	}

	public void assertMosaicsOwned(final Collection<Mosaic> returnedMosaics, final List<Mosaic> expectedMosaics) {
		Assert.assertThat(returnedMosaics.size(), IsEqual.equalTo(expectedMosaics.size()));
		Assert.assertThat(returnedMosaics, IsEquivalent.equivalentTo(expectedMosaics));
	}
}
