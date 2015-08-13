package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AccountNamespaceInfoControllerTest {

	@Test
	public void accountGetMosaicDefinitionsDelegatesToNamespaceCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId1 = context.createMosaicId("gimre.games.pong", "Paddle");
		final MosaicId mosaicId2 = context.createMosaicId("gimre.games.pong", "Ball");
		final MosaicId mosaicId3 = context.createMosaicId("gimre.games.pong", "Goals");
		context.prepareMosaics(Arrays.asList(mosaicId1, mosaicId2, mosaicId3));
		context.ownsMosaic(context.address, Arrays.asList(mosaicId1, mosaicId2));
		final Address another = Utils.generateRandomAddressWithPublicKey();
		context.ownsMosaic(another, Arrays.asList(mosaicId2, mosaicId3));

		// Act:
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions1 = this.getAccountMosaicDefinitions(context, context.address);
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions2 = this.getAccountMosaicDefinitions(context, another);

		// Assert:
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.address);
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(another);
		context.assertMosaicsOwned(returnedMosaicDefinitions1.asCollection(), Arrays.asList(mosaicId1, mosaicId2));
		context.assertMosaicsOwned(returnedMosaicDefinitions2.asCollection(), Arrays.asList(mosaicId2, mosaicId3));
	}

	@Test
	public void accountGetMosaicDefinitionsBatchDelegatesToNamespaceCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId1 = context.createMosaicId("gimre.games.pong", "Paddle");
		final MosaicId mosaicId2 = context.createMosaicId("gimre.games.pong", "Ball");
		final MosaicId mosaicId3 = context.createMosaicId("gimre.games.pong", "Goals");
		context.prepareMosaics(Arrays.asList(mosaicId1, mosaicId2, mosaicId3));
		context.ownsMosaic(context.address, Arrays.asList(mosaicId1, mosaicId2));
		final Address another = Utils.generateRandomAddressWithPublicKey();
		context.ownsMosaic(another, Arrays.asList(mosaicId2, mosaicId3));

		// Act:
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions = this.getAccountMosaicDefinitionsBatch(
				context,
				Arrays.asList(context.address, another));

		// Assert:
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.address);
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(another);
		context.assertMosaicsOwned(returnedMosaicDefinitions.asCollection(), Arrays.asList(mosaicId1, mosaicId2, mosaicId3));
	}

	private SerializableList<MosaicDefinition> getAccountMosaicDefinitions(final TestContext context, final Address address) {
		return context.controller.accountGetMosaicDefinitions(context.getBuilder(address));
	}

	private SerializableList<MosaicDefinition> getAccountMosaicDefinitionsBatch(final TestContext context, final List<Address> addresses) {
		final Collection<AccountId> accountIds = addresses.stream().map(a -> new AccountId(a.getEncoded())).collect(Collectors.toList());
		return context.controller.accountGetMosaicDefinitionsBatch(NisUtils.getAccountIdsDeserializer(accountIds));
	}

	private static class TestContext {
		private final Address address = Utils.generateRandomAddressWithPublicKey();

		private final AccountNamespaceInfoController controller;
		private final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final ReadOnlyNamespaceCache namespaceCache = Mockito.mock(ReadOnlyNamespaceCache.class);
		private final HashMap<MosaicId, MosaicDefinition> mosaicDefinitions = new HashMap<>();

		public TestContext() {
			this.controller = new AccountNamespaceInfoController(
					this.accountStateCache,
					this.namespaceCache);
		}

		private AccountIdBuilder getBuilder(final Address address) {
			final AccountIdBuilder builder = new AccountIdBuilder();
			builder.setAddress(address.getEncoded());
			return builder;
		}

		public MosaicId createMosaicId(final String namespaceName, final String mosaicName) {
			final MosaicId mosaicId = Utils.createMosaicId(namespaceName, mosaicName);
			this.mosaicDefinitions.put(mosaicId, Mockito.mock(MosaicDefinition.class));
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

				final MosaicEntry mosaicEntry = Mockito.mock(MosaicEntry.class);
				Mockito.when(mosaics.get(mosaicId)).thenReturn(mosaicEntry);

				final MosaicDefinition mosaicDefinition = this.mosaicDefinitions.get(mosaicId);
				Mockito.when(mosaicEntry.getMosaicDefinition()).thenReturn(mosaicDefinition);
			}
		}

		public void ownsMosaic(final Address address, final List<MosaicId> mosaicIds) {
			final ReadOnlyAccountState accountState = Mockito.mock(AccountState.class);
			final org.nem.nis.state.AccountInfo accountInfo = Mockito.mock(org.nem.nis.state.AccountInfo.class);

			Mockito.when(accountState.getAccountInfo()).thenReturn(accountInfo);
			Mockito.when(accountInfo.getMosaicIds()).thenReturn(mosaicIds);
			Mockito.when(this.accountStateCache.findStateByAddress(address)).thenReturn(accountState);
		}

		public void assertMosaicsOwned(final Collection<MosaicDefinition> returnedMosaicDefinitions, final List<MosaicId> expected) {
			Assert.assertThat(returnedMosaicDefinitions.size(), IsEqual.equalTo(expected.size()));

			final Set<MosaicDefinition> definitions = expected.stream().map(this.mosaicDefinitions::get).collect(Collectors.toSet());
			Assert.assertThat(returnedMosaicDefinitions, IsEquivalent.equivalentTo(definitions));
		}
	}
}