package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.MockTransaction;
import org.nem.nis.test.DebitPredicates;

public class UniversalTransactionValidatorTest {
	private static final SingleTransactionValidator VALIDATOR = new UniversalTransactionValidator();

	@Test
	public void transactionWithDeadlineInRangeIsValid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(726));

		// Assert:
		Assert.assertThat(validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionWithLessThanMinimumDeadlineIsInvalid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp());

		// Assert:
		Assert.assertThat(validate(transaction), IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}

	@Test
	public void transactionWithMinimumDeadlineIsValid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));

		// Assert:
		Assert.assertThat(validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionWithMaximumDeadlineIsValid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addDays(1));

		// Assert:
		Assert.assertThat(validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionWithGreaterThanMaximumDeadlineIsInvalid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addDays(1).addSeconds(1));

		// Assert:
		Assert.assertThat(validate(transaction), IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
	}

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
		transaction.setFee(Amount.fromNem(120));
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(726));

		final DebitPredicate debitPredicate = Mockito.mock(DebitPredicate.class);
		Mockito.when(debitPredicate.canDebit(Mockito.any(), Mockito.any())).thenReturn(predicateResult);

		// Act:
		final ValidationResult result = validate(transaction, debitPredicate);

		// Assert:
		Mockito.verify(debitPredicate, Mockito.only()).canDebit(transaction.getSigner(), Amount.fromNem(120));
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	private static ValidationResult validate(final Transaction transaction) {
		return validate(transaction, DebitPredicates.True);
	}

	private static ValidationResult validate(final Transaction transaction, final DebitPredicate debitPredicate) {
		return VALIDATOR.validate(transaction, new ValidationContext(debitPredicate));
	}
}