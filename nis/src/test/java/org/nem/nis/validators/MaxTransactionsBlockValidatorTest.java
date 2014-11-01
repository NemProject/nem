package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.MockTransaction;
import org.nem.nis.secret.BlockChainConstants;
import org.nem.nis.test.NisUtils;

public class MaxTransactionsBlockValidatorTest {
	private static final BlockValidator VALIDATOR = new MaxTransactionsBlockValidator();
	private static final Block BLOCK = NisUtils.createRandomBlock();

	@Test
	public void validateReturnsSuccessWhenBlockHasLessThanOrEqualToMaximumTransactions() {
		// Arrange:
		this.addTransactions(BLOCK, BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Assert:
		Assert.assertThat(VALIDATOR.validate(BLOCK), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsFailureWhenBlockHasMoreThanMaximumTransactions() {
		// Arrange:
		this.addTransactions(BLOCK, BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK + 1);

		// Assert:
		Assert.assertThat(VALIDATOR.validate(BLOCK), IsEqual.equalTo(ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS));
	}

	private Block addTransactions(final Block block, final int numTransactions) {
		for (int i=0; i<numTransactions; i++) {
			block.addTransaction(new MockTransaction());
		}

		return block;
	}
}
