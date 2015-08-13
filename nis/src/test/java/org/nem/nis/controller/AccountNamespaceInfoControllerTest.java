package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
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
		final ThreeMosaicsTestContext context = new ThreeMosaicsTestContext();

		// Act:
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions1 = this.getAccountMosaicDefinitions(context, context.address);
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions2 = this.getAccountMosaicDefinitions(context, context.another);

		// Assert:
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.address);
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.another);
		context.assertMosaicDefinitionsOwned(returnedMosaicDefinitions1.asCollection(), Arrays.asList(context.mosaicId1, context.mosaicId2));
		context.assertMosaicDefinitionsOwned(returnedMosaicDefinitions2.asCollection(), Arrays.asList(context.mosaicId2, context.mosaicId3));
	}

	@Test
	public void accountGetMosaicDefinitionsBatchDelegatesToNamespaceCache() {
		// Arrange:
		final ThreeMosaicsTestContext context = new ThreeMosaicsTestContext();

		// Act:
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions = this.getAccountMosaicDefinitionsBatch(
				context,
				Arrays.asList(context.address, context.another));

		// Assert:
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.address);
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.another);
		context.assertMosaicDefinitionsOwned(returnedMosaicDefinitions.asCollection(), Arrays.asList(context.mosaicId1, context.mosaicId2, context.mosaicId3));
	}

	@Test
	public void accountGetOwnedMosaicsDelegatesToNamespaceCache() {
		// Arrange:
		final ThreeMosaicsTestContext context = new ThreeMosaicsTestContext();
		context.setBalance(context.mosaicId1, context.address, new Quantity(123));
		context.setBalance(context.mosaicId1, context.another, new Quantity(789));
		context.setBalance(context.mosaicId3, context.address, new Quantity(456));
		context.setBalance(context.mosaicId2, context.another, new Quantity(528));

		// Act:
		final SerializableList<Mosaic> returnedMosaics1 = this.getOwnedMosaics(context, context.address);
		final SerializableList<Mosaic> returnedMosaics2 = this.getOwnedMosaics(context, context.another);

		// Assert:
		// - note that the returned mosaics are based on what the account is reported to own via its info
		// - in "production" zero-balance mosaics should be excluded out and non-zero-balance mosaics should be included
		// - but this is a test where we do not have that constraint
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.address);
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findStateByAddress(context.another);
		context.assertMosaicsOwned(
				returnedMosaics1.asCollection(),
				Arrays.asList(new Mosaic(context.mosaicId1, new Quantity(123)), new Mosaic(context.mosaicId2, Quantity.ZERO)));
		context.assertMosaicsOwned(
				returnedMosaics2.asCollection(),
				Arrays.asList(new Mosaic(context.mosaicId2, new Quantity(528)), new Mosaic(context.mosaicId3, Quantity.ZERO)));
	}

	private SerializableList<Mosaic> getOwnedMosaics(final TestContext context, final Address address) {
		return context.controller.accountGetOwnedMosaics(context.getBuilder(address));
	}

	private SerializableList<MosaicDefinition> getAccountMosaicDefinitions(final TestContext context, final Address address) {
		return context.controller.accountGetMosaicDefinitions(context.getBuilder(address));
	}

	private SerializableList<MosaicDefinition> getAccountMosaicDefinitionsBatch(final TestContext context, final List<Address> addresses) {
		final Collection<AccountId> accountIds = addresses.stream().map(a -> new AccountId(a.getEncoded())).collect(Collectors.toList());
		return context.controller.accountGetMosaicDefinitionsBatch(NisUtils.getAccountIdsDeserializer(accountIds));
	}

	private static class ThreeMosaicsTestContext extends TestContext {
		private final MosaicId mosaicId1 = this.createMosaicId("gimre.games.pong", "Paddle");
		private final MosaicId mosaicId2 = this.createMosaicId("gimre.games.pong", "Ball");
		private final MosaicId mosaicId3 = this.createMosaicId("gimre.games.pong", "Goals");
		private final Address another = Utils.generateRandomAddressWithPublicKey();

		public ThreeMosaicsTestContext() {
			this.prepareMosaics(Arrays.asList(this.mosaicId1, this.mosaicId2, this.mosaicId3));
			this.ownsMosaic(this.address, Arrays.asList(this.mosaicId1, this.mosaicId2));
			this.ownsMosaic(this.another, Arrays.asList(this.mosaicId2, this.mosaicId3));
		}
	}

	private static class TestContext {
		public final Address address = Utils.generateRandomAddressWithPublicKey();

		private final AccountNamespaceInfoController controller;
		public final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final NamespaceCache namespaceCache = Mockito.mock(NamespaceCache.class);
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
			final MosaicDefinition mosaicDefinition = new MosaicDefinition(
					Utils.generateRandomAccount(),
					mosaicId,
					new MosaicDescriptor("descriptor"),
					Utils.createMosaicProperties(),
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
}