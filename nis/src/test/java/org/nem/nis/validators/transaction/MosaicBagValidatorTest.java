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

public class MosaicBagValidatorTest {
	private static final Supply INITIAL_SUPPLY = new Supply(10000);
	private static final Quantity INITIAL_QUANTITY = new Quantity(100000000);
	private static final BlockHeight VALIDATION_HEIGHT = new BlockHeight(21);
	private static final Amount FIVE_XEM = Amount.fromNem(5);
	private static final Amount ONE_POINT_TWO_XEM = Amount.fromNem(1).add(Amount.fromMicroNem(Amount.MICRONEMS_IN_NEM / 5));

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
		final TransferTransaction transaction = context.createTransaction(FIVE_XEM, idInTransaction, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.FAILURE_MOSAIC_UNKNOWN);
	}

	// endregion

	// region unknown levy

	@Test
	public void transactionIsInvalidIfMosaicLevyIsUnknown() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition("foo", "tokens");
		final TestContext context = new TestContext();
		context.addMosaicDefinition(context.createMosaicDefinitionWithUnknownLevy(mosaicDefinition.getId()));
		final TransferTransaction transaction = context.createTransaction(FIVE_XEM, mosaicDefinition.getId(), 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.FAILURE_MOSAIC_LEVY_UNKNOWN);
	}

	// endregion

	// region expired namespace

	@Test
	public void transactionIsInvalidIfNamespaceIsNotActive() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId), VALIDATION_HEIGHT.next());
		final TransferTransaction transaction = context.createTransaction(FIVE_XEM, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.FAILURE_NAMESPACE_EXPIRED);
	}

	// endregion

	// region transferable

	@Test
	public void transactionIsValidIfMosaicIsNotTransferableAndSenderIsTheMosaicCreator() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account recipient = Utils.generateRandomAccount();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId, createMosaicProperties(false)));
		final TransferTransaction transaction = context.createTransaction(context.signer, recipient, FIVE_XEM, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionIsValidIfMosaicIsNotTransferableAndRecipientIsTheMosaicCreator() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId, createMosaicProperties(false)));
		context.transfer(mosaicId, context.signer.getAddress(), sender.getAddress(), new Quantity(7500));
		final TransferTransaction transaction = context.createTransaction(sender, context.signer, FIVE_XEM, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionIsInvalidIfMosaicIsNotTransferableNeitherSignerNorRecipientIsTheMosaicCreator() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId, createMosaicProperties(false)));
		final TransferTransaction transaction = context.createTransaction(sender, recipient, FIVE_XEM, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.FAILURE_MOSAIC_NOT_TRANSFERABLE);
	}

	// endregion

	// region fractional amount

	@Test
	public void transactionWithFractionalAmountAndNoMosaicsValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = context.createTransaction(ONE_POINT_TWO_XEM, null);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithFractionalAmountAndMosaicsDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId));
		final TransferTransaction transaction = context.createTransaction(ONE_POINT_TWO_XEM, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.FAILURE_MOSAIC_DIVISIBILITY_VIOLATED);
	}

	// endregion

	// region max mosaics

	@Test
	public void transactionWithMaxMosaicsValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = context.createTransactionWithNumMosaics(10);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithTooManyMosaicsDoesNotValidate() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = context.createTransactionWithNumMosaics(11);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.FAILURE_TOO_MANY_MOSAIC_TRANSFERS);
	}

	// endregion

	// region valid

	@Test
	public void transactionWithNoMosaicsValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = context.createTransaction(FIVE_XEM, null);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithValidMosaicsValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId));
		final TransferTransaction transaction = context.createTransaction(FIVE_XEM, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithValidMosaicsValidatesWhenCompleteBalanceIsTransferred() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaicDefinition(context.createMosaicDefinition(mosaicId));
		final TransferTransaction transaction = context.createTransaction(FIVE_XEM, mosaicId, INITIAL_QUANTITY.getRaw() / 5);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	// endregion

	private static MosaicProperties createMosaicProperties(final boolean transferable) {
		return Utils.createMosaicProperties(INITIAL_SUPPLY.getRaw(), 4, null, transferable);
	}

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final NamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		private final MosaicBagValidator validator = new MosaicBagValidator(this.namespaceCache);

		public void addMosaicDefinition(final MosaicDefinition mosaicDefinition) {
			this.addMosaicDefinition(mosaicDefinition, VALIDATION_HEIGHT);
		}

		public void addMosaicDefinition(final MosaicDefinition mosaicDefinition, final BlockHeight namespaceHeight) {
			final Namespace namespace = new Namespace(mosaicDefinition.getId().getNamespaceId(), mosaicDefinition.getCreator(),
					namespaceHeight);
			this.namespaceCache.add(namespace);
			this.namespaceCache.get(namespace.getId()).getMosaics().add(mosaicDefinition);
		}

		public void transfer(final MosaicId mosaicId, final Address sender, final Address recipient, final Quantity quantity) {
			final MosaicEntry entry = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
			entry.getBalances().decrementBalance(sender, quantity);
			entry.getBalances().incrementBalance(recipient, quantity);
		}

		private ValidationResult validate(final TransferTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(VALIDATION_HEIGHT, ValidationStates.Throw));
		}

		private MosaicDefinition createMosaicDefinition(final MosaicId mosaicId) {
			return this.createMosaicDefinition(mosaicId, createMosaicProperties(true));
		}

		private MosaicDefinition createMosaicDefinition(final MosaicId mosaicId, final MosaicProperties properties) {
			return Utils.createMosaicDefinition(this.signer, mosaicId, properties);
		}

		private MosaicDefinition createMosaicDefinitionWithUnknownLevy(final MosaicId mosaicId) {
			return Utils.createMosaicDefinition(this.signer, mosaicId, createMosaicProperties(true), Utils.createMosaicLevy());
		}

		// region createTransaction

		private TransferTransaction createTransaction(final Account signer, final Account recipient, final Amount amount,
				final MosaicId mosaicId, final long quantity) {
			// Arrange: add three mosaic definitions with the "interesting" one in the middle
			final MosaicDefinition firstMosaicDefinition = this.addTestMosaicDefinition(signer.getAddress(), Utils.createMosaicId(1));
			final MosaicDefinition lastMosaicDefinition = this.addTestMosaicDefinition(signer.getAddress(), Utils.createMosaicId(3));

			final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
			attachment.addMosaic(firstMosaicDefinition.getId(), new Quantity(111));
			attachment.addMosaic(mosaicId, new Quantity(quantity));
			attachment.addMosaic(lastMosaicDefinition.getId(), new Quantity(333));
			return createTransaction(signer, recipient, amount, attachment);
		}

		private MosaicDefinition addTestMosaicDefinition(final Address senderAddress, final MosaicId mosaicId) {
			final MosaicDefinition mosaicDefinition = this.createMosaicDefinition(mosaicId);
			this.addMosaicDefinition(mosaicDefinition);

			// if the test mosaic signer is different from the sender, transfer the full balance to the sender
			if (!senderAddress.equals(this.signer.getAddress())) {
				this.transfer(mosaicId, this.signer.getAddress(), senderAddress, INITIAL_QUANTITY);
			}

			return mosaicDefinition;
		}

		private TransferTransaction createTransaction(final Amount amount, final MosaicId mosaicId, final long quantity) {
			return this.createTransaction(this.signer, this.recipient, amount, mosaicId, quantity);
		}

		private TransferTransaction createTransaction(final Amount amount, final TransferTransactionAttachment attachment) {
			return createTransaction(this.signer, this.recipient, amount, attachment);
		}

		private TransferTransaction createTransactionWithNumMosaics(final int numMosaics) {
			final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
			for (int i = 0; i < numMosaics; ++i) {
				final MosaicId mosaicId = Utils.createMosaicId(i + 1);
				this.addMosaicDefinition(this.createMosaicDefinition(mosaicId));
				attachment.addMosaic(mosaicId, new Quantity(1));
			}

			return this.createTransaction(Amount.fromNem(1), attachment);
		}

		private static TransferTransaction createTransaction(final Account signer, final Account recipient, final Amount amount,
				final TransferTransactionAttachment attachment) {
			return new TransferTransaction(TimeInstant.ZERO, signer, recipient, amount, attachment);
		}

		private void assertValidationResult(final TransferTransaction transaction, final ValidationResult expectedResult) {
			// Act:
			final ValidationResult result = this.validate(transaction);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
		}

		// endregion
	}
}
