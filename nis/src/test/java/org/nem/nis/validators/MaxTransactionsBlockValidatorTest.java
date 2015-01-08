package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.MockTransaction;
import org.nem.nis.test.NisUtils;

public class MaxTransactionsBlockValidatorTest {
	private static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = 120;
	private static final long TEST_HEIGHT = 123;

	// region tests

	@Test
	public void validateReturnsSuccessWhenBlockHasLessThanMaximumTransactions() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 1,
				TEST_HEIGHT,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasExactlyMaximumTransactions() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				MAX_ALLOWED_TRANSACTIONS_PER_BLOCK,
				TEST_HEIGHT,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsFailureWhenBlockHasMoreThanMaximumTransactions() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(
				MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1,
				TEST_HEIGHT,
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
