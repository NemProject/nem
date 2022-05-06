package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.RandomTransactionFactory;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

public class TSingleTransactionValidatorAdapterTest {

	@Test
	public void getNameDelegatesToInnerValidator() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.innerValidator.getName()).thenReturn("inner");

		// Act:
		final String name = context.validator.getName();

		// Assert:
		MatcherAssert.assertThat(name, IsEqual.equalTo("inner"));
		Mockito.verify(context.innerValidator, Mockito.only()).getName();
	}

	@Test
	public void validateDelegatesToInnerValidatorForSupportedTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = RandomTransactionFactory.createTransfer();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_UNKNOWN));
		Mockito.verify(context.innerValidator, Mockito.only()).validate(Mockito.any(), Mockito.any());
	}

	@Test
	public void validateBypassesInnerValidatorForUnsupportedTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = RandomTransactionFactory.createImportanceTransfer();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Mockito.verify(context.innerValidator, Mockito.never()).validate(Mockito.any(), Mockito.any());
	}

	private static class TestContext {
		@SuppressWarnings("unchecked")
		private final TSingleTransactionValidator<TransferTransaction> innerValidator = Mockito.mock(TSingleTransactionValidator.class);

		private final SingleTransactionValidator validator = new TSingleTransactionValidatorAdapter<>(TransactionTypes.TRANSFER,
				this.innerValidator);

		public TestContext() {
			Mockito.when(this.innerValidator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.FAILURE_UNKNOWN);
		}

		public ValidationResult validate(final Transaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(ValidationStates.Throw));
		}
	}
}
