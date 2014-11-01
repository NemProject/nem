package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.MockTransaction;
import org.nem.nis.secret.BlockChainConstants;
import org.nem.nis.test.NisUtils;

public class MaxTransactionsBlockValidatorTest {
	private static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK;

	@Test
	public void validateReturnsSuccessWhenBlockHasLessThanMaximumTransactions() {
		// Assert:
		Assert.assertThat(
				validateBlockWithTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK),
				IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsSuccessWhenBlockHasExactlyMaximumTransactions() {
		// Assert:
		Assert.assertThat(
				validateBlockWithTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK),
				IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsFailureWhenBlockHasMoreThanMaximumTransactions() {
		// Assert:
		Assert.assertThat(
				validateBlockWithTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1),
				IsEqual.equalTo(ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS));
	}

	private static ValidationResult validateBlockWithTransactions(final int numTransactions) {
		// Arrange:
		final BlockValidator validator = new MaxTransactionsBlockValidator();
		final Block block = NisUtils.createRandomBlock();
		for (int i=0; i<numTransactions; i++) {
			block.addTransaction(new MockTransaction());
		}

		// Act:
		return validator.validate(block);
	}
}
