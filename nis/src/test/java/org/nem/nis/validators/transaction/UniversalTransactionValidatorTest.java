package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

import java.util.function.Function;

public class UniversalTransactionValidatorTest {

	//region timestamp < deadline <= timestamp + 1 day

	@Test
	public void transactionWithDeadlineInRangeIsValid() {
		// Assert:
		assertTimeStampDeadlineValidation(ts -> ts.addSeconds(726), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithLessThanMinimumDeadlineIsInvalid() {
		// Assert:
		assertTimeStampDeadlineValidation(ts -> ts, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	@Test
	public void transactionWithMinimumDeadlineIsValid() {
		// Assert:
		assertTimeStampDeadlineValidation(ts -> ts.addSeconds(1), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithMaximumDeadlineIsValid() {
		// Assert:
		assertTimeStampDeadlineValidation(ts -> ts.addDays(1), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithGreaterThanMaximumDeadlineIsInvalid() {
		// Assert:
		assertTimeStampDeadlineValidation(ts -> ts.addDays(1).addSeconds(1), ValidationResult.FAILURE_FUTURE_DEADLINE);
	}

	private static void assertTimeStampDeadlineValidation(
			final Function<TimeInstant, TimeInstant> getDeadlineFromTimeStamp,
			final ValidationResult expectedResult) {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(getDeadlineFromTimeStamp.apply(transaction.getTimeStamp()));

		// Act:
		final ValidationResult result = validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	//region minimum fee validation

	@Test
	public void transactionWithInvalidFeeFailsValidation() {
		// Assert:
		assertValidationResultForMockTransactionFee(
				MockTransaction.DEFAULT_FEE.subtract(Amount.fromNem(1)),
				ValidationResult.FAILURE_INSUFFICIENT_FEE);
	}

	@Test
	public void transactionWithValidFeePassesValidation() {
		// Assert:
		assertValidationResultForMockTransactionFee(
				MockTransaction.DEFAULT_FEE,
				ValidationResult.SUCCESS);
	}

	private static void assertValidationResultForMockTransactionFee(final Amount fee, final ValidationResult expectedResult) {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setFee(fee);

		// Act:
		final ValidationResult result = validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	@Test
	public void transferTransactionIncludingMosaicsWithInvalidFeeFailsValidation() {
		// Assert:
		assertValidationResultForTransferTransactionWithMosaicFee(Amount.fromNem(192), ValidationResult.FAILURE_INSUFFICIENT_FEE);
	}

	@Test
	public void transferTransactionIncludingMosaicsWithValidFeePassesValidation() {
		// Assert:
		assertValidationResultForTransferTransactionWithMosaicFee(Amount.fromNem(193), ValidationResult.SUCCESS);
	}

	private static void assertValidationResultForTransferTransactionWithMosaicFee(final Amount fee, final ValidationResult expectedResult) {
		// Arrange:
		final Account namespaceOwner = Utils.generateRandomAccount();
		final MosaicId mosaicId = Utils.createMosaicId(1);
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(namespaceOwner, mosaicId, Utils.createMosaicProperties(10000L, 0, null, null));
		final NamespaceCache namespaceCache = new DefaultNamespaceCache();
		namespaceCache.add(new Namespace(mosaicId.getNamespaceId(), namespaceOwner, BlockHeight.ONE));
		namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().add(mosaicDefinition);

		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaicId, new Quantity(1000));
		final TransferTransaction transaction = new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(1),
				attachment);
		transaction.setFee(fee);
		transaction.setDeadline(new TimeInstant(1));

		final SingleTransactionValidator validator = new UniversalTransactionValidator(namespaceCache);

		// Act:
		final ValidationResult result = validator.validate(transaction, new ValidationContext(ValidationStates.Throw));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	private static ValidationResult validate(final Transaction transaction) {
		// Arrange:
		final SingleTransactionValidator validator = new UniversalTransactionValidator(new DefaultNamespaceCache());

		// Act:
		return validator.validate(transaction, new ValidationContext(ValidationStates.Throw));
	}
}