package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.function.BiFunction;

public class NonConflictingImportanceTransferTransactionValidatorTest {

	//region success

	@Test
	public void importanceTransferIsValidWhenExistingTransactionsAreEmpty() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final Transaction transaction = createImportanceTransferTransaction(signer, remote);
		final SingleTransactionValidator validator = new NonConflictingImportanceTransferTransactionValidator(ArrayList::new);

		// Act:
		final ValidationResult result = validate(validator, transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void importanceTransferIsValidWhenSignerConflictsWithExistingNonImportanceTransfer() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final Transaction transaction = createImportanceTransferTransaction(signer, remote);
		final List<Transaction> transactions = Arrays.asList(new MockTransaction(signer, 1));
		final SingleTransactionValidator validator = new NonConflictingImportanceTransferTransactionValidator(() -> transactions);

		// Act:
		final ValidationResult result = validate(validator, transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region conflicts
	@Test
	public void importanceTransferIsNotConflictingWithItself() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final Transaction transaction = createImportanceTransferTransaction(signer, remote);
		final List<Transaction> transactions = Arrays.asList(transaction);
		final SingleTransactionValidator validator = new NonConflictingImportanceTransferTransactionValidator(() -> transactions);

		// Act:
		final ValidationResult result = validate(validator, transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void importanceTransferIsNotValidWhenIsSharesSignerWithExistingImportanceTransfer() {
		// Assert:
		assertValidationFailure((signer, remote) -> createImportanceTransferTransaction(Utils.generateRandomAccount(), remote));
	}

	@Test
	public void importanceTransferIsNotValidWhenIsSharesRemoteWithExistingImportanceTransfer() {
		// Assert:
		assertValidationFailure((signer, remote) -> createImportanceTransferTransaction(Utils.generateRandomAccount(), remote));
	}

	@Test
	public void importanceTransferIsNotValidWhenIsSharesSignerAndRemoteWithExistingImportanceTransfer() {
		// Assert:
		assertValidationFailure((signer, remote) -> createImportanceTransferTransaction(signer, remote));
	}

	private static void assertValidationFailure(final BiFunction<Account, Account, Transaction> createConflictingTransaction) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final Transaction transaction = createImportanceTransferTransaction(signer, remote);

		final List<Transaction> transactions = Arrays.asList(
				createImportanceTransferTransaction(Utils.generateRandomAccount(), Utils.generateRandomAccount()),
				createConflictingTransaction.apply(signer, remote),
				createImportanceTransferTransaction(Utils.generateRandomAccount(), Utils.generateRandomAccount()));
		final SingleTransactionValidator validator = new NonConflictingImportanceTransferTransactionValidator(() -> transactions);

		// Act:
		final ValidationResult result = validate(validator, transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CONFLICTING_IMPORTANCE_TRANSFER));
	}

	//endregion

	//region other type

	@Test
	public void otherTransactionTypesPassValidationWithSharedSigner() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final List<Transaction> transactions = Arrays.asList(new MockTransaction(signer, 1));
		final SingleTransactionValidator validator = new NonConflictingImportanceTransferTransactionValidator(() -> transactions);

		// Act:
		final ValidationResult result = validate(validator, new MockTransaction(signer, 2));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	private static Transaction createImportanceTransferTransaction(final Account signer, final Account remote) {
		return new ImportanceTransferTransaction(TimeInstant.ZERO, signer, ImportanceTransferTransaction.Mode.Activate, remote);
	}

	private static ValidationResult validate(final SingleTransactionValidator validator, final Transaction transaction) {
		return validator.validate(transaction, new ValidationContext(DebitPredicate.True));
	}
}