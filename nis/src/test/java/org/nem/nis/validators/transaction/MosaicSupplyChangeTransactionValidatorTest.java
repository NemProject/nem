package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.state.MosaicEntry;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.ValidationContext;

public class MosaicSupplyChangeTransactionValidatorTest {
	private static final long INITIAL_SUPPLY = 10000;
	private static final long MAX_SUPPLY = MosaicConstants.MAX_QUANTITY / 100000;
	private static final BlockHeight VALIDATION_HEIGHT = new BlockHeight(21);

	// region valid

	@Test
	public void createMosaicsIncreasingSupplyValidates() {
		// Assert:
		assertValidTransactionWithType(MosaicSupplyType.Create, 1234);
	}

	@Test
	public void createMosaicsIncreasingSupplyToMaxValidates() {
		// Assert:
		assertValidTransactionWithType(MosaicSupplyType.Create, MAX_SUPPLY - INITIAL_SUPPLY);
	}

	@Test
	public void deleteMosaicsDecreasingSupplyValidates() {
		// Assert:
		assertValidTransactionWithType(MosaicSupplyType.Delete, 1234);
	}

	@Test
	public void deleteMosaicsDecreasingSupplyToZeroValidates() {
		// Assert:
		assertValidTransactionWithType(MosaicSupplyType.Delete, INITIAL_SUPPLY);
	}

	@Test
	public void deleteMosaicsDecreasingOwnerSupplyToZeroValidates() {
		// Assert:
		assertValidTransactionWithType(MosaicSupplyType.Delete, INITIAL_SUPPLY / 2, INITIAL_SUPPLY / 2);
	}

	private static void assertValidTransactionWithType(final MosaicSupplyType supplyType, final long quantity) {
		// Assert:
		assertValidTransactionWithType(supplyType, quantity, quantity);
	}

	private static void assertValidTransactionWithType(final MosaicSupplyType supplyType, final long quantity, final long ownerQuantity) {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId), VALIDATION_HEIGHT, new Supply(ownerQuantity));
		final MosaicSupplyChangeTransaction transaction = context.createTransaction(mosaicId, supplyType, quantity);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	// endregion

	// region unknown mosaic

	@Test
	public void transactionIsInvalidIfNamespaceIdIsUnknown() {
		// Assert:
		assertUnknownMosaic(Utils.createMosaicDefinition("foo", "tokens").getId(), Utils.createMosaicDefinition("bar", "tokens").getId());
	}

	@Test
	public void transactionIsInvalidIfMosaicIdIsUnknown() {
		// Assert:
		assertUnknownMosaic(Utils.createMosaicDefinition("foo", "tokens").getId(), Utils.createMosaicDefinition("foo", "coins").getId());
	}

	private static void assertUnknownMosaic(final MosaicId idInCache, final MosaicId idInTransaction) {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinition(context.createMosaicDefinition(idInCache));
		final MosaicSupplyChangeTransaction transaction = context.createTransaction(idInTransaction, 1234);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_UNKNOWN));
	}

	// endregion

	// region expired namespace

	@Test
	public void transactionIsInvalidIfNamespaceIsNotActive() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId), VALIDATION_HEIGHT.next());
		final MosaicSupplyChangeTransaction transaction = context.createTransaction(mosaicId, 1234);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_EXPIRED));
	}

	// endregion

	// region creator conflict

	@Test
	public void transactionIsInvalidIfMosaicCreatorDiffersFromTransactionSigner() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(
				Utils.createMosaicDefinition(Utils.generateRandomAccount(), mosaicId, createMosaicProperties(INITIAL_SUPPLY, true)));
		final MosaicSupplyChangeTransaction transaction = context.createTransaction(mosaicId, 1234);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_CREATOR_CONFLICT));
	}

	// endregion

	// region immutable

	@Test
	public void createMosaicsSupplyTypeForImmutableMosaicIsInvalid() {
		// Assert:
		assertInvalidForImmutableMosaic(MosaicSupplyType.Create);
	}

	@Test
	public void deleteMosaicsSupplyTypeForImmutableMosaicIsInvalid() {
		// Assert:
		assertInvalidForImmutableMosaic(MosaicSupplyType.Delete);
	}

	private static void assertInvalidForImmutableMosaic(final MosaicSupplyType supplyType) {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId, createMosaicProperties(INITIAL_SUPPLY, false)));
		final MosaicSupplyChangeTransaction transaction = context.createTransaction(mosaicId, supplyType, 1234);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_SUPPLY_IMMUTABLE));
	}

	// endregion

	// region quantity bounds

	@Test
	public void createSupplyTransactionIsInvalidIfMaxQuantityIsExceeded() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId));
		final MosaicSupplyChangeTransaction transaction = context.createTransaction(mosaicId, MAX_SUPPLY - INITIAL_SUPPLY + 1);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_MAX_SUPPLY_EXCEEDED));
	}

	@Test
	public void deleteSupplyTransactionIsInvalidIfResultingTotalSupplyIsNegative() {
		// Assert:
		assertNegativeDelete(INITIAL_SUPPLY, INITIAL_SUPPLY + 1);
	}

	@Test
	public void deleteSupplyTransactionIsInvalidIfOwnerDoesNotHaveCorrespondingMosaic() {
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
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId), VALIDATION_HEIGHT, new Supply(creatorSupply));
		final MosaicSupplyChangeTransaction transaction = context.createTransaction(mosaicId, MosaicSupplyType.Delete, decreaseSupply);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_SUPPLY_NEGATIVE));
	}

	// endregion

	private static MosaicProperties createMosaicProperties(final long quantity, final boolean mutableSupply) {
		return Utils.createMosaicProperties(quantity, 5, mutableSupply, null);
	}

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final NamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		private final MosaicSupplyChangeTransactionValidator validator = new MosaicSupplyChangeTransactionValidator(this.namespaceCache);

		public void addMosaicDefinition(final MosaicDefinition mosaicDefinition) {
			this.addMosaicDefinition(mosaicDefinition, VALIDATION_HEIGHT);
		}

		public void addMosaicDefinition(final MosaicDefinition mosaicDefinition, final BlockHeight namespaceHeight) {
			this.addMosaicDefinition(mosaicDefinition, namespaceHeight, new Supply(mosaicDefinition.getProperties().getInitialSupply()));
		}

		public void addMosaicDefinition(final MosaicDefinition mosaicDefinition, final BlockHeight namespaceHeight,
				final Supply creatorSupply) {
			final Namespace namespace = new Namespace(mosaicDefinition.getId().getNamespaceId(), mosaicDefinition.getCreator(),
					namespaceHeight);
			this.namespaceCache.add(namespace);
			final MosaicEntry entry = this.namespaceCache.get(namespace.getId()).getMosaics().add(mosaicDefinition);

			final Address creatorAddress = mosaicDefinition.getCreator().getAddress();
			entry.getBalances().decrementBalance(creatorAddress, entry.getBalances().getBalance(creatorAddress));
			entry.getBalances().incrementBalance(creatorAddress, MosaicUtils.toQuantity(creatorSupply, 5));
		}

		private ValidationResult validate(final MosaicSupplyChangeTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(VALIDATION_HEIGHT, ValidationStates.Throw));
		}

		private MosaicDefinition createMosaicDefinition(final MosaicId mosaicId) {
			return this.createMosaicDefinition(mosaicId, createMosaicProperties(INITIAL_SUPPLY, true));
		}

		private MosaicDefinition createMosaicDefinition(final MosaicId mosaicId, final MosaicProperties properties) {
			return Utils.createMosaicDefinition(this.signer, mosaicId, properties);
		}

		private MosaicSupplyChangeTransaction createTransaction(final MosaicId mosaicId, final long quantity) {
			return this.createTransaction(mosaicId, MosaicSupplyType.Create, quantity);
		}

		private MosaicSupplyChangeTransaction createTransaction(final MosaicId mosaicId, final MosaicSupplyType supplyType,
				final long quantity) {
			return new MosaicSupplyChangeTransaction(TimeInstant.ZERO, this.signer, mosaicId, supplyType, new Supply(quantity));
		}
	}
}
