package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.state.MosaicEntry;
import org.nem.nis.test.*;
import org.nem.nis.validators.ValidationContext;

import java.util.*;

public class SmartTileSupplyChangeTransactionValidatorTest {
	private static final long INITIAL_SUPPLY = 10000;
	private static final long MAX_SUPPLY = MosaicConstants.MAX_QUANTITY / 10000;
	private static final BlockHeight VALIDATION_HEIGHT = new BlockHeight(21);

	//region valid

	@Test
	public void createSmartTilesIncreasingSupplyToValidates() {
		// Assert:
		assertValidTransactionWithType(SmartTileSupplyType.CreateSmartTiles, 1234);
	}

	@Test
	public void createSmartTilesIncreasingSupplyToMaxValidates() {
		// Assert:
		assertValidTransactionWithType(SmartTileSupplyType.CreateSmartTiles, MAX_SUPPLY - INITIAL_SUPPLY);
	}

	@Test
	public void deleteSmartTilesDecreasingSupplyToValidates() {
		// Assert:
		assertValidTransactionWithType(SmartTileSupplyType.DeleteSmartTiles, 1234);
	}

	@Test
	public void deleteSmartTilesDecreasingSupplyToZeroValidates() {
		// Assert:
		assertValidTransactionWithType(SmartTileSupplyType.DeleteSmartTiles, INITIAL_SUPPLY);
	}

	@Test
	public void deleteSmartTilesDecreasingOwnerSupplyToZeroValidates() {
		// Assert:
		assertValidTransactionWithType(SmartTileSupplyType.DeleteSmartTiles, INITIAL_SUPPLY / 2, INITIAL_SUPPLY / 2);
	}

	private static void assertValidTransactionWithType(final SmartTileSupplyType supplyType, final long quantity) {
		// Assert:
		assertValidTransactionWithType(supplyType, quantity, quantity);
	}

	private static void assertValidTransactionWithType(final SmartTileSupplyType supplyType, final long quantity, final long ownerQuantity) {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId), VALIDATION_HEIGHT, new Quantity(ownerQuantity));
		final SmartTileSupplyChangeTransaction transaction = context.createTransaction(mosaicId, supplyType, quantity);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region unknown mosaic

	@Test
	public void transactionIsInvalidIfNamespaceIdIsUnknown() {
		// Assert:
		assertUnknownMosaic(Utils.createMosaic("foo", "tokens").getId(), Utils.createMosaic("bar", "tokens").getId());
	}

	@Test
	public void transactionIsInvalidIfMosaicIdIsUnknown() {
		// Assert:
		assertUnknownMosaic(Utils.createMosaic("foo", "tokens").getId(), Utils.createMosaic("foo", "coins").getId());
	}

	private static void assertUnknownMosaic(final MosaicId idInCache, final MosaicId idInTransaction) {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaic(context.createMosaic(idInCache));
		final SmartTileSupplyChangeTransaction transaction = context.createTransaction(idInTransaction, 1234);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_UNKNOWN));
	}

	//endregion

	//region expired namespace

	@Test
	public void transactionIsInvalidIfNamespaceIsNotActive() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId), VALIDATION_HEIGHT.next());
		final SmartTileSupplyChangeTransaction transaction = context.createTransaction(mosaicId, 1234);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_EXPIRED));
	}

	//endregion

	//region creator conflict

	@Test
	public void transactionIsInvalidIfMosaicCreatorDiffersFromTransactionSigner() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(Utils.createMosaic(Utils.generateRandomAccount(), mosaicId, createMosaicProperties(INITIAL_SUPPLY, true)));
		final SmartTileSupplyChangeTransaction transaction = context.createTransaction(mosaicId, 1234);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_CREATOR_CONFLICT));
	}

	//endregion

	//region immutable

	@Test
	public void createSmartTilesSupplyTypeForImmutableMosaicIsInvalid() {
		// Assert:
		assertInvalidForImmutableMosaic(SmartTileSupplyType.CreateSmartTiles);
	}

	@Test
	public void deleteSmartTilesSupplyTypeForImmutableMosaicIsInvalid() {
		// Assert:
		assertInvalidForImmutableMosaic(SmartTileSupplyType.DeleteSmartTiles);
	}

	private static void assertInvalidForImmutableMosaic(final SmartTileSupplyType supplyType) {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId, createMosaicProperties(INITIAL_SUPPLY, false)));
		final SmartTileSupplyChangeTransaction transaction = context.createTransaction(mosaicId, supplyType, 1234);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_QUANTITY_IMMUTABLE));
	}

	//endregion

	//region quantity bounds

	@Test
	public void createSupplyTransactionIsInvalidIfMaxQuantityIsExceeded() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId));
		final SmartTileSupplyChangeTransaction transaction = context.createTransaction(mosaicId, MAX_SUPPLY - INITIAL_SUPPLY + 1);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_MAX_QUANTITY_EXCEEDED));
	}

	@Test
	public void deleteSupplyTransactionIsInvalidIfResultingTotalSupplyIsNegative() {
		// Assert:
		assertNegativeDelete(INITIAL_SUPPLY, INITIAL_SUPPLY + 1);
	}

	@Test
	public void deleteSupplyTransactionIsInvalidIfOwnerDoesNotHaveCorrespondingSmartTile() {
		// Assert:
		assertNegativeDelete(0, 1);
	}

	@Test
	public void deleteSupplyTransactionIsInvalidIfResultingOwnerSupplyIsNegative() {
		// Assert:
		assertNegativeDelete(INITIAL_SUPPLY / 2, INITIAL_SUPPLY / 2 + 1);
	}

	private static void assertNegativeDelete(final long creatorSupply, final long decreaseSupply) {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId), VALIDATION_HEIGHT, new Quantity(creatorSupply));
		final SmartTileSupplyChangeTransaction transaction = context.createTransaction(
				mosaicId,
				SmartTileSupplyType.DeleteSmartTiles,
				decreaseSupply);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_QUANTITY_NEGATIVE));
	}

	//endregion

	private static MosaicProperties createMosaicProperties(final long quantity, final boolean mutableQuantity) {
		final Properties properties = new Properties();
		properties.put("quantity", String.valueOf(quantity));
		properties.put("mutablequantity", mutableQuantity ? "true" : "false");
		properties.put("divisibility", "4");
		return new DefaultMosaicProperties(properties);
	}

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final NamespaceCache namespaceCache = new DefaultNamespaceCache();
		private final SmartTileSupplyChangeTransactionValidator validator = new SmartTileSupplyChangeTransactionValidator(this.namespaceCache);

		public void addMosaic(final Mosaic mosaic) {
			this.addMosaic(mosaic, VALIDATION_HEIGHT);
		}

		public void addMosaic(final Mosaic mosaic, final BlockHeight namespaceHeight) {
			this.addMosaic(mosaic, namespaceHeight, new Quantity(mosaic.getProperties().getInitialQuantity()));
		}

		public void addMosaic(final Mosaic mosaic, final BlockHeight namespaceHeight, final Quantity creatorSupply) {
			final Namespace namespace = new Namespace(mosaic.getId().getNamespaceId(), mosaic.getCreator(), namespaceHeight);
			this.namespaceCache.add(namespace);
			final MosaicEntry entry = this.namespaceCache.get(namespace.getId()).getMosaics().add(mosaic);

			final Address creatorAddress = mosaic.getCreator().getAddress();
			entry.getBalances().decrementBalance(creatorAddress, entry.getBalances().getBalance(creatorAddress));
			entry.getBalances().incrementBalance(creatorAddress, creatorSupply);
		}

		private ValidationResult validate(final SmartTileSupplyChangeTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(VALIDATION_HEIGHT, DebitPredicates.Throw));
		}

		private Mosaic createMosaic(final MosaicId mosaicId) {
			return this.createMosaic(mosaicId, createMosaicProperties(INITIAL_SUPPLY, true));
		}

		private Mosaic createMosaic(final MosaicId mosaicId, final MosaicProperties properties) {
			return Utils.createMosaic(this.signer, mosaicId, properties);
		}

		private SmartTileSupplyChangeTransaction createTransaction(
				final MosaicId mosaicId,
				final long quantity) {
			return this.createTransaction(
					mosaicId,
					SmartTileSupplyType.CreateSmartTiles,
					quantity);
		}

		private SmartTileSupplyChangeTransaction createTransaction(
				final MosaicId mosaicId,
				final SmartTileSupplyType supplyType,
				final long quantity) {
			return new SmartTileSupplyChangeTransaction(
					TimeInstant.ZERO,
					this.signer,
					mosaicId,
					supplyType,
					new Quantity(quantity));
		}
	}
}
