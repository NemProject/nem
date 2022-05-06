package org.nem.nis.validators.block;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.MockTransaction;
import org.nem.nis.test.*;
import org.nem.nis.validators.BlockValidator;

import java.util.*;

public class MaxTransactionsBlockValidatorTest {
	private static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = NisTestConstants.MAX_TRANSACTIONS_PER_BLOCK;
	private static final long TEST_HEIGHT = 123;

	// region no child transactions

	@Test
	public void validateReturnsSuccessWhenBlockHasLessThanMaximumTransactions() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 1, TEST_HEIGHT,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasExactlyMaximumTransactions() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, TEST_HEIGHT,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsFailureWhenBlockHasMoreThanMaximumTransactions() {
		assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1, TEST_HEIGHT,
				ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS);
	}

	private static void assertValidationResultWhenBlockHasAGivenNumberOfTransactionsAtHeight(final int numTransactions,
			final long forkHeight, final ValidationResult expectedResult) {
		// Arrange:
		final BlockValidator validator = new MaxTransactionsBlockValidator();
		final Block block = NisUtils.createRandomBlockWithHeight(forkHeight);
		for (int i = 0; i < numTransactions; i++) {
			block.addTransaction(new MockTransaction());
		}

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region child transactions

	@Test
	public void validateReturnsSuccessWhenBlockHasLessThanMaximumTransactionsIncludingChildTransactions() {
		assertValidationResultWhenBlockHasAGivenNumberOfChildTransactionsAtHeight(1, MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 2, TEST_HEIGHT,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasExactlyMaximumTransactionsIncludingChildTransactions() {
		assertValidationResultWhenBlockHasAGivenNumberOfChildTransactionsAtHeight(1, MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 1, TEST_HEIGHT,
				ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsFailureWhenBlockHasMoreThanMaximumTransactionsIncludingChildTransactions() {
		assertValidationResultWhenBlockHasAGivenNumberOfChildTransactionsAtHeight(1, MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, TEST_HEIGHT,
				ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS);
	}

	private static void assertValidationResultWhenBlockHasAGivenNumberOfChildTransactionsAtHeight(final int numTransactions,
			final int numChildTransactions, final long forkHeight, final ValidationResult expectedResult) {
		// Arrange:
		final BlockValidator validator = new MaxTransactionsBlockValidator();
		final Block block = NisUtils.createRandomBlockWithHeight(forkHeight);
		for (int i = 0; i < numTransactions; i++) {
			final List<Transaction> childTransactions = new ArrayList<>();
			for (int j = 0; j < numChildTransactions; ++j) {
				childTransactions.add(new MockTransaction());
			}

			final MockTransaction transaction = new MockTransaction();
			transaction.setChildTransactions(childTransactions);
			block.addTransaction(transaction);
		}

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion
}
