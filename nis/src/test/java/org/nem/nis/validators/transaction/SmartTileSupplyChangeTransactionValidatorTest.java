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
import org.nem.nis.state.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.ValidationContext;

import java.util.*;

public class SmartTileSupplyChangeTransactionValidatorTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final BlockHeight VALIDATION_HEIGHT = new BlockHeight(21);

	@Test
	public void validTransactionValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		assertValidationResultForTransaction(
				context,
				SmartTileSupplyType.CreateSmartTiles,
				"foo",
				1000,
				ValidationResult.SUCCESS);
	}

	@Test
	public void transactionIsInvalidIfMosaicIdIsUnknown() {
		// Arrange:
		final TestContext context = new TestContext();
		assertValidationResultForTransaction(
				context,
				SmartTileSupplyType.CreateSmartTiles,
				"bar",
				1000,
				ValidationResult.FAILURE_MOSAIC_UNKNOWN);
	}

	@Test
	public void transactionIsInvalidIfMosaicCreatorDiffersFromTransactionSigner() {
		// Arrange:
		final TestContext context = new TestContext(Utils.generateRandomAccount());
		assertValidationResultForTransaction(
				context,
				SmartTileSupplyType.CreateSmartTiles,
				"foo",
				1000,
				ValidationResult.FAILURE_MOSAIC_CREATOR_CONFLICT);
	}

	@Test
	public void transactionIsInvalidIfMaxQuantityIsExceeded() {
		// Arrange:
		final TestContext context = new TestContext();
		assertValidationResultForTransaction(
				context,
				SmartTileSupplyType.CreateSmartTiles,
				"foo",
				10000,
				ValidationResult.FAILURE_MOSAIC_MAX_QUANTITY_EXCEEDED);
	}

	@Test
	public void transactionIsInvalidIfResultingQuantityIsNegative() {
		// Arrange:
		final TestContext context = new TestContext();
		assertValidationResultForTransaction(
				context,
				SmartTileSupplyType.DeleteSmartTiles,
				"foo",
				1000,
				ValidationResult.FAILURE_MOSAIC_QUANTITY_NEGATIVE);
	}

	@Test
	public void transactionIsInvalidIfQuantityIsImmutableAndThereIsAlreadySupply() {
		// Arrange:
		final TestContext context = new TestContext(false);
		context.addSmartTile();
		assertValidationResultForTransaction(
				context,
				SmartTileSupplyType.CreateSmartTiles,
				"foo",
				1000,
				ValidationResult.FAILURE_MOSAIC_QUANTITY_IMMUTABLE);
	}

	@Test
	public void transactionIsInvalidIfNamespaceIsNotActive() {
		// Arrange:
		final TestContext context = new TestContext(new BlockHeight(123));
		assertValidationResultForTransaction(
				context,
				SmartTileSupplyType.CreateSmartTiles,
				"foo",
				1000,
				ValidationResult.FAILURE_NAMESPACE_EXPIRED);
	}

	private static void assertValidationResultForTransaction(
			final TestContext context,
			final SmartTileSupplyType supplyType,
			final String namespace,
			final long quantity,
			final ValidationResult expectedResult) {
		final SmartTileSupplyChangeTransaction transaction = createTransaction(
				supplyType,
				namespace,
				Quantity.fromValue(quantity));

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static SmartTileSupplyChangeTransaction createTransaction(
			final SmartTileSupplyType supplyType,
			final String namespace,
			final Quantity quantity) {
		return new SmartTileSupplyChangeTransaction(
				TimeInstant.ZERO,
				SIGNER,
				new MosaicId(new NamespaceId(namespace), "bar"),
				supplyType,
				quantity);
	}

	private static Mosaic createMosaic(
			final Account creator,
			final long quantity,
			final boolean mutableQuantity) {
		final Properties properties = new Properties();
		properties.put("quantity", String.valueOf(quantity));
		properties.put("mutablequantity", mutableQuantity ? "true" : "false");
		final MosaicProperties mosaicProperties = new DefaultMosaicProperties(properties);
		return new Mosaic(
				creator,
				new MosaicId(new NamespaceId("foo"), "bar"),
				new MosaicDescriptor("baz"),
				mosaicProperties);

	}

	private class TestContext {
		final Mosaic mosaic;
		final BlockHeight validationHeight;
		final NisCache nisCache = NisCacheFactory.createReal().copy();
		final SmartTileMap map = new SmartTileMap();
		final SmartTileSupplyChangeTransactionValidator validator = new SmartTileSupplyChangeTransactionValidator(this.nisCache);

		private TestContext() {
			this(SIGNER, 1000, true, VALIDATION_HEIGHT);
		}

		private TestContext(final Account creator) {
			this(creator, 1000, true, VALIDATION_HEIGHT);
		}

		private TestContext(final boolean mutableQuantity) {
			this(SIGNER, 1000, mutableQuantity, VALIDATION_HEIGHT);
		}

		private TestContext(final BlockHeight validationHeight) {
			this(SIGNER, 1000, true, validationHeight);
		}

		private TestContext(
				final Account creator,
				final long quantity,
				final boolean mutableQuantity,
				final BlockHeight validationHeight) {
			this.mosaic = createMosaic(creator, quantity, mutableQuantity);
			this.validationHeight = validationHeight;
			this.setupCache();
		}

		private void setupCache() {
			final Namespace namespace = new Namespace(this.mosaic.getId().getNamespaceId(), this.mosaic.getCreator(), this.validationHeight);
			this.nisCache.getNamespaceCache().add(namespace);
			this.nisCache.getNamespaceCache().get(namespace.getId()).getMosaics().add(this.mosaic);
		}

		private void addSmartTile() {
			final SmartTile smartTile = new SmartTile(new MosaicId(new NamespaceId("foo"), "bar"), Quantity.fromValue(100));
			this.map.add(smartTile);
		}

		private ValidationResult validate(final SmartTileSupplyChangeTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(VALIDATION_HEIGHT, DebitPredicates.Throw));
		}
	}
}
