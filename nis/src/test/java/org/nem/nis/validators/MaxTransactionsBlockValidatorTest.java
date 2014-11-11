package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.MockTransaction;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.NisUtils;

public class MaxTransactionsBlockValidatorTest {
	private static final long BETA_HARD_FORK = BlockMarkerConstants.BETA_HARD_FORK;
	private static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = 60;
	private static final long BETA_TX_HARD_FORK = BlockMarkerConstants.BETA_TX_COUNT_FORK;
	private static final int NEW_MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = 120;

	// region first hard fork

	@Test
	public void validateReturnsSuccessWhenBlockHasLessThanMaximumTransactionsAfterForkHeight() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 1,
				BETA_HARD_FORK + 1,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasExactlyMaximumTransactionsAfterForkHeight() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				MAX_ALLOWED_TRANSACTIONS_PER_BLOCK,
				BETA_HARD_FORK + 1,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsFailureWhenBlockHasMoreThanMaximumTransactionsAfterForkHeight() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1,
				BETA_HARD_FORK + 1,
				ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS);
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasMoreThanMaximumTransactionsAtForkHeight() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1,
				BETA_HARD_FORK,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasMoreThanMaximumTransactionsBeforeForkHeight() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1,
				BETA_HARD_FORK - 1,
				ValidationResult.SUCCESS);
	}

	// endregion

	// region second hard fork

	@Test
	public void validateReturnsSuccessWhenBlockHasLessThanMaximumTransactionsAfterSecondForkHeight() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				NEW_MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 1,
				BETA_TX_HARD_FORK + 1,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasExactlyMaximumTransactionsAfterSecondForkHeight() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				NEW_MAX_ALLOWED_TRANSACTIONS_PER_BLOCK,
				BETA_TX_HARD_FORK + 1,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsFailureWhenBlockHasMoreThanMaximumTransactionsAfterSecondForkHeight() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				NEW_MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1,
				BETA_TX_HARD_FORK + 1,
				ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS);
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasExactlyFirstForkMaximumTransactionsAtSecondForkHeight() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				MAX_ALLOWED_TRANSACTIONS_PER_BLOCK,
				BETA_TX_HARD_FORK,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsFailureWhenBlockHasMoreThanFirstForkMaximumTransactionsAtSecondForkHeight() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1,
				BETA_TX_HARD_FORK,
				ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS);
	}

	// endregion

	private static void assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
			final int maxAllowedTransactions,
			final long forkHeight,
			final ValidationResult result) {
		// Assert:
		Assert.assertThat(
				validateBlockWithTransactions(maxAllowedTransactions, forkHeight),
				IsEqual.equalTo(result));
	}

	private static ValidationResult validateBlockWithTransactions(final int numTransactions, final long height) {
		// Arrange:
		final BlockValidator validator = new MaxTransactionsBlockValidator();
		final Block block = NisUtils.createRandomBlockWithHeight(height);
		for (int i = 0; i < numTransactions; i++) {
			block.addTransaction(new MockTransaction());
		}

		// Act:
		return validator.validate(block);
	}
}
