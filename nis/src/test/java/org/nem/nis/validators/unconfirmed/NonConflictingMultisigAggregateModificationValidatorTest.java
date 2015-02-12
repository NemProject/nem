package org.nem.nis.validators.unconfirmed;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.stream.*;

public class NonConflictingMultisigAggregateModificationValidatorTest {

	//region passes validation

	@Test
	public void nonModificationTransactionsWithSameSignerValidates() {
		// Assert:
		final Account signer = Utils.generateRandomAccount();
		assertValidation(
				Arrays.asList(new MockTransaction(signer)),
				new MockTransaction(signer),
				ValidationResult.SUCCESS);
	}

	@Test
	public void firstModificationValidates() {
		// Assert:
		final Account signer = Utils.generateRandomAccount();
		assertValidation(
				Arrays.asList(),
				createModificationTransaction(signer),
				ValidationResult.SUCCESS);
	}

	@Test
	public void singleModificationAndMultipleNonModificationTransactionsWithSameSignerValidates() {
		// Assert:
		final Account signer = Utils.generateRandomAccount();
		assertValidation(
				Arrays.asList(new MockTransaction(signer), new MockTransaction(signer)),
				createModificationTransaction(signer),
				ValidationResult.SUCCESS);
	}

	@Test
	public void multipleModificationsWithDifferentSignerValidates() {
		// Assert:
		assertValidation(
				IntStream.range(0, 3).mapToObj(i -> createModificationTransaction(Utils.generateRandomAccount())).collect(Collectors.toList()),
				createModificationTransaction(Utils.generateRandomAccount()),
				ValidationResult.SUCCESS);
	}

	//endregion

	//region fails validation

	@Test
	public void newModificationWithSameSignerAsExistingModificationDoesNotValidate() {
		// Assert:
		final Account signer = Utils.generateRandomAccount();
		assertValidation(
				Arrays.asList(createModificationTransaction(signer)),
				createModificationTransaction(signer),
				ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION);
	}

	//endregion

	private static Transaction createModificationTransaction(final Account signer) {
		return new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				signer,
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, Utils.generateRandomAccount())));
	}

	private static void assertValidation(
			final Collection<Transaction> existingTransactions,
			final Transaction newTransaction,
			final ValidationResult expectedResult) {
		// Arrange:
		final SingleTransactionValidator validator = new NonConflictingMultisigAggregateModificationValidator(() -> existingTransactions.stream());

		// Act:
		final ValidationResult result = validator.validate(newTransaction, new ValidationContext(DebitPredicates.True));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}