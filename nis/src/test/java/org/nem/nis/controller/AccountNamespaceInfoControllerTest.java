package org.nem.nis.controller;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.Utils;
import org.nem.nis.test.*;

import java.util.*;
import java.util.stream.Collectors;

public class AccountNamespaceInfoControllerTest {

	// region accountGetMosaicDefinitionsDelegatesToNamespaceCache

	@Test
	public void accountGetMosaicDefinitionsDelegatesToNamespaceCache() {
		// Arrange:
		final ThreeMosaicsWithNoLeviesTestContext context = new ThreeMosaicsWithNoLeviesTestContext();

		// Act:
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions1 = this.getAccountMosaicDefinitions(context, context.address);
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions2 = this.getAccountMosaicDefinitions(context, context.another);

		// Assert:
		context.assertAccountStateDelegation();
		context.assertNamespaceCacheNumGetDelegations(5); // three from first call and two from second
		context.assertMosaicDefinitionsOwned(returnedMosaicDefinitions1.asCollection(),
				Arrays.asList(MosaicConstants.MOSAIC_ID_XEM, context.mosaicId1, context.mosaicId2, context.mosaicId4));
		context.assertMosaicDefinitionsOwned(returnedMosaicDefinitions2.asCollection(),
				Arrays.asList(MosaicConstants.MOSAIC_ID_XEM, context.mosaicId2, context.mosaicId3));
	}

	@Test
	public void accountGetMosaicDefinitionsDelegatesToNamespaceCacheWhenSomeMosaicsHaveLevies() {
		// Arrange:
		final ThreeMosaicsWithLeviesTestContext context = new ThreeMosaicsWithLeviesTestContext();

		// Act:
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions1 = this.getAccountMosaicDefinitions(context, context.address);
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions2 = this.getAccountMosaicDefinitions(context, context.another);

		// Assert:
		context.assertAccountStateDelegation();
		context.assertNamespaceCacheNumGetDelegations(9); // five from first call and four from second
		context.assertMosaicDefinitionsOwned(returnedMosaicDefinitions1.asCollection(),
				Arrays.asList(MosaicConstants.MOSAIC_ID_XEM, context.mosaicId1, context.mosaicId2, context.mosaicId4, context.mosaic1Levy));
		context.assertMosaicDefinitionsOwned(returnedMosaicDefinitions2.asCollection(),
				Arrays.asList(MosaicConstants.MOSAIC_ID_XEM, context.mosaicId2, context.mosaicId3, context.mosaicId1));
	}

	// endregion

	// region accountGetMosaicDefinitionsBatchDelegatesToNamespaceCache

	@Test
	public void accountGetMosaicDefinitionsBatchDelegatesToNamespaceCache() {
		// Arrange:
		final ThreeMosaicsWithNoLeviesTestContext context = new ThreeMosaicsWithNoLeviesTestContext();

		// Act:
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions = this.getAccountMosaicDefinitionsBatch(context,
				Arrays.asList(context.address, context.another));

		// Assert:
		context.assertAccountStateDelegation();
		context.assertNamespaceCacheNumGetDelegations(5);
		context.assertMosaicDefinitionsOwned(returnedMosaicDefinitions.asCollection(),
				Arrays.asList(MosaicConstants.MOSAIC_ID_XEM, context.mosaicId1, context.mosaicId2, context.mosaicId3, context.mosaicId4));
	}

	@Test
	public void accountGetMosaicDefinitionsBatchDelegatesToNamespaceCacheWhenSomeMosaicsHaveLevies() {
		// Arrange:
		final ThreeMosaicsWithLeviesTestContext context = new ThreeMosaicsWithLeviesTestContext();

		// Act:
		final SerializableList<MosaicDefinition> returnedMosaicDefinitions = this.getAccountMosaicDefinitionsBatch(context,
				Arrays.asList(context.address, context.another));

		// Assert:
		context.assertAccountStateDelegation();
		context.assertNamespaceCacheNumGetDelegations(9);
		context.assertMosaicDefinitionsOwned(returnedMosaicDefinitions.asCollection(), Arrays.asList(MosaicConstants.MOSAIC_ID_XEM,
				context.mosaicId1, context.mosaicId2, context.mosaicId3, context.mosaicId4, context.mosaic1Levy));
	}

	// endregion

	// region accountGetOwnedMosaicsDelegatesToNamespaceCache

	@Test
	public void accountGetOwnedMosaicsDelegatesToNamespaceCache() {
		// Arrange:
		final ThreeMosaicsWithNoLeviesTestContext context = new ThreeMosaicsWithNoLeviesTestContext();
		context.setBalance(context.mosaicId1, context.address, new Quantity(123));
		context.setBalance(context.mosaicId1, context.another, new Quantity(789));
		context.setBalance(context.mosaicId3, context.address, new Quantity(456));
		context.setBalance(context.mosaicId2, context.another, new Quantity(528));
		context.incrementXemBalance(context.address, Amount.fromNem(444));

		// Sanity:
		// 1) 5 calls in prepareMosaics() during context creation (gets added in assertNamespaceCacheNumGetDelegations())
		// 2) 4 calls when setting the balance for mosaicId1, ..., mosaicId4
		context.assertNamespaceCacheNumGetDelegations(4);

		// Act:
		final SerializableList<Mosaic> returnedMosaics1 = this.getOwnedMosaics(context, context.address);
		final SerializableList<Mosaic> returnedMosaics2 = this.getOwnedMosaics(context, context.another);

		// Assert:
		// - note that the returned mosaics are based on what the account is reported to own via its info
		// - in "production" zero-balance mosaics should be excluded out and non-zero-balance mosaics should be included
		// - but this is a test where we do not have that constraint
		context.assertAccountStateDelegation();

		// TODO 20151207 J-G: really this should move to tests for MosaicInfoFactory (which is now injected to this class)
		// 3) 6 calls in getOwnedMosaics() for context.address
		// 4) 4 calls in getOwnedMosaics() for context.another
		context.assertNamespaceCacheNumGetDelegations(4 + 6 + 4);
		context.assertMosaicsOwned(returnedMosaics1.asCollection(),
				Arrays.asList(new Mosaic(MosaicConstants.MOSAIC_ID_XEM, new Quantity(444000000)),
						new Mosaic(context.mosaicId1, new Quantity(123)), new Mosaic(context.mosaicId2, Quantity.ZERO),
						new Mosaic(context.mosaicId4, Quantity.ZERO)));
		context.assertMosaicsOwned(returnedMosaics2.asCollection(), Arrays.asList(new Mosaic(MosaicConstants.MOSAIC_ID_XEM, Quantity.ZERO),
				new Mosaic(context.mosaicId2, new Quantity(528)), new Mosaic(context.mosaicId3, Quantity.ZERO)));
	}

	// endregion

	private SerializableList<Mosaic> getOwnedMosaics(final ThreeMosaicsTestContext context, final Address address) {
		return context.controller.accountGetOwnedMosaics(context.getBuilder(address));
	}

	private SerializableList<MosaicDefinition> getAccountMosaicDefinitions(final ThreeMosaicsTestContext context, final Address address) {
		return context.controller.accountGetMosaicDefinitions(context.getBuilder(address));
	}

	private SerializableList<MosaicDefinition> getAccountMosaicDefinitionsBatch(final ThreeMosaicsTestContext context,
			final List<Address> addresses) {
		final Collection<AccountId> accountIds = addresses.stream().map(a -> new AccountId(a.getEncoded())).collect(Collectors.toList());
		return context.controller.accountGetMosaicDefinitionsBatch(NisUtils.getAccountIdsDeserializer(accountIds));
	}

	private static abstract class ThreeMosaicsTestContext extends MosaicTestContext {
		protected final Address address = Utils.generateRandomAddressWithPublicKey();
		protected final Address another = Utils.generateRandomAddressWithPublicKey();

		private final AccountNamespaceInfoController controller = new AccountNamespaceInfoController(this.mosaicInfoFactory);

		public abstract int numMosaics();

		public AccountIdBuilder getBuilder(final Address address) {
			final AccountIdBuilder builder = new AccountIdBuilder();
			builder.setAddress(address.getEncoded());
			return builder;
		}

		public void assertAccountStateDelegation() {
			Mockito.verify(this.accountStateCache, Mockito.times(1)).findStateByAddress(this.address);
			Mockito.verify(this.accountStateCache, Mockito.times(1)).findStateByAddress(this.another);
		}

		public void assertNamespaceCacheNumGetDelegations(final int count) {
			// numMosaics get calls were made by prepareMosaics in the constructor
			Mockito.verify(this.namespaceCache, Mockito.times(this.numMosaics() + count)).get(Mockito.any());
		}
	}

	private static class ThreeMosaicsWithNoLeviesTestContext extends ThreeMosaicsTestContext {
		private final MosaicId mosaicId1 = this.createMosaicId("gimre.games.pong", "paddle");
		private final MosaicId mosaicId2 = this.createMosaicId("gimre.games.pong", "ball");
		private final MosaicId mosaicId3 = this.createMosaicId("gimre.games.pong", "goal");
		private final MosaicId mosaicId4 = this.createMosaicId("gimre.games.pong", "game");

		public ThreeMosaicsWithNoLeviesTestContext() {
			this.addXemMosaic();
			this.prepareMosaics(
					Arrays.asList(MosaicConstants.MOSAIC_ID_XEM, this.mosaicId1, this.mosaicId2, this.mosaicId3, this.mosaicId4));
			this.ownsMosaic(this.address, Arrays.asList(this.mosaicId1, this.mosaicId2, this.mosaicId4));
			this.ownsMosaic(this.another, Arrays.asList(this.mosaicId2, this.mosaicId3));
		}

		@Override
		public int numMosaics() {
			return 4 + 1; // XEM mosaic is always present
		}
	}

	private static class ThreeMosaicsWithLeviesTestContext extends ThreeMosaicsTestContext {
		private final MosaicId mosaic1Levy = this.createMosaicId("gimre.games.pong", "ball-levy");
		private final MosaicId mosaicId1 = this.createMosaicId("gimre.games.pong", "paddle", 0L,
				new MosaicLevy(MosaicTransferFeeType.Absolute, Utils.generateRandomAccount(), this.mosaic1Levy, new Quantity(11)));
		private final MosaicId mosaicId2 = this.createMosaicId("gimre.games.pong", "ball");
		private final MosaicId mosaicId3 = this.createMosaicId("gimre.games.pong", "goal", 0L,
				new MosaicLevy(MosaicTransferFeeType.Absolute, Utils.generateRandomAccount(), this.mosaicId1, new Quantity(11)));
		private final MosaicId mosaicId4 = this.createMosaicId("gimre.games.pong", "game");

		public ThreeMosaicsWithLeviesTestContext() {
			this.addXemMosaic();
			this.prepareMosaics(Arrays.asList(this.mosaicId1, this.mosaicId2, this.mosaicId3, this.mosaicId4, this.mosaic1Levy));
			this.ownsMosaic(this.address, Arrays.asList(this.mosaicId1, this.mosaicId2, this.mosaicId4));
			this.ownsMosaic(this.another, Arrays.asList(this.mosaicId2, this.mosaicId3));
		}

		@Override
		public int numMosaics() {
			return 5;
		}
	}
}
