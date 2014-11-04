package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.MockTransaction;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.test.NisUtils;

public class MaxTransactionsBlockValidatorTest {
	private static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK;
	private static final long BETA_HARD_FORK = BlockMarkerConstants.BETA_HARD_FORK;

	@Test
	public void validateReturnsSuccessWhenBlockHasLessThanMaximumTransactionsAfterForkHeight() {
		// Assert:
		Assert.assertThat(
				validateBlockWithTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK),
				IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasExactlyMaximumTransactionsAfterForkHeight() {
		// Assert:
		Assert.assertThat(
				validateBlockWithTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK),
				IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsFailureWhenBlockHasMoreThanMaximumTransactionsAfterForkHeight() {
		// Assert:
		Assert.assertThat(
				validateBlockWithTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1),
				IsEqual.equalTo(ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS));
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasMoreThanMaximumTransactionsAtForkHeight() {
		// Assert:
		Assert.assertThat(
				validateBlockWithTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1, BETA_HARD_FORK),
				IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasMoreThanMaximumTransactionsBeforeForkHeight() {
		// Assert:
		Assert.assertThat(
				validateBlockWithTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1, BETA_HARD_FORK - 1),
				IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	private static ValidationResult validateBlockWithTransactions(final int numTransactions) {
		// Act:
		return validateBlockWithTransactions(numTransactions, BETA_HARD_FORK + 1);
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
