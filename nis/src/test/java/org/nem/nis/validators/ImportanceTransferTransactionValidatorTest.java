package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.poi.PoiFacade;

public class ImportanceTransferTransactionValidatorTest {

	@Test
	public void activateImportanceTransferIsValid() {
		// Arrange:
		final Transaction transaction = createTransaction(ImportanceTransferTransaction.Mode.Activate);

		// Assert:
		Assert.assertThat(createValidator().validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void deactivateImportanceTransferIsValid() {
		// Arrange:
		final Transaction transaction = createTransaction(ImportanceTransferTransaction.Mode.Activate);

		// Assert:
		Assert.assertThat(createValidator().validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void otherImportanceTransferIsNotValid() {
		// Arrange:
		final Transaction transaction = createTransaction(ImportanceTransferTransaction.Mode.Unknown);

		// Assert:
		Assert.assertThat(createValidator().validate(transaction), IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
	}

	//region other type

	@Test
	public void otherTransactionTypesPassValidation() {
		// Arrange:
		final Account account = Utils.generateRandomAccount(Amount.fromNem(100));
		final MockTransaction transaction = new MockTransaction(account);
		transaction.setFee(Amount.fromNem(200));

		// Assert:
		Assert.assertThat(createValidator().validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	private static TransactionValidator createValidator() {
		return new ImportanceTransferTransactionValidator(Mockito.mock(PoiFacade.class));
	}

	private static ImportanceTransferTransaction createTransaction(final ImportanceTransferTransaction.Mode mode) {
		return new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				mode, Utils.generateRandomAccount());
	}
}