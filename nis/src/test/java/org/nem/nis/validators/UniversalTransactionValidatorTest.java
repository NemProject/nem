package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.MockTransaction;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.DebitPredicates;

import java.util.function.Function;

public class UniversalTransactionValidatorTest {
	private static final SingleTransactionValidator VALIDATOR = new UniversalTransactionValidator();

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

	//region debit predicate delegation

	@Test
	public void validatorDelegatesToDebitPredicateWithFeeAndUsesResultWhenDebitPredicateSucceeds() {
		// Assert:
		assertDebitPredicateDelegation(true, ValidationResult.SUCCESS);
	}

	@Test
	public void validatorDelegatesToDebitPredicateWithFeeAndUsesResultWhenDebitPredicateFails() {
		// Assert:
		assertDebitPredicateDelegation(false, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	private static void assertDebitPredicateDelegation(final boolean predicateResult, final ValidationResult expectedValidationResult) {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setUseRandomDebtor(true);
		transaction.setFee(Amount.fromNem(120));
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(726));

		final DebitPredicate debitPredicate = Mockito.mock(DebitPredicate.class);
		Mockito.when(debitPredicate.canDebit(Mockito.any(), Mockito.any())).thenReturn(predicateResult);

		// Act:
		final ValidationResult result = validate(transaction, debitPredicate);

		// Assert:
		Mockito.verify(debitPredicate, Mockito.only()).canDebit(transaction.getDebtor(), Amount.fromNem(120));
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	//endregion

	//region minimum fee validation

	// TODO 20150129 J-*: consider using PowerMock

	@Test
	public void transactionWithInvalidFeeFailsValidation() {
		// Assert:
		assertValidationResult(
				MockTransaction.DEFAULT_FEE.subtract(Amount.fromNem(1)),
				ValidationResult.FAILURE_INSUFFICIENT_FEE);
	}

	@Test
	public void transactionWithValidFeePassesValidates() {
		// Assert:
		assertValidationResult(
				MockTransaction.DEFAULT_FEE,
				ValidationResult.SUCCESS);
	}

	private static void assertValidationResult(final Amount fee, final ValidationResult expectedResult) {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setFee(fee);

		// Act:
		final ValidationResult result = validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	private static ValidationResult validate(final Transaction transaction) {
		return validate(transaction, DebitPredicates.True);
	}

	private static ValidationResult validate(final Transaction transaction, final DebitPredicate debitPredicate) {
		return VALIDATOR.validate(transaction, new ValidationContext(debitPredicate));
	}
}