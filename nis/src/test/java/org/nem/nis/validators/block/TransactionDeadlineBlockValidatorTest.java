package org.nem.nis.validators.block;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.MockTransaction;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.NisUtils;

public class TransactionDeadlineBlockValidatorTest {
	private static final long TEST_HEIGHT = 123;
	private static final TransactionDeadlineBlockValidator VALIDATOR = new TransactionDeadlineBlockValidator();

	@Test
	public void validateReturnsFailureIfBlockContainsTransactionWithDeadlineEarlierThanBlockTimeStamp() {
		assertBlockValidationResult(123, TEST_HEIGHT, 122, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	@Test
	public void validateReturnsSuccessIfBlockContainsTransactionWithDeadlineEqualToBlockTimeStamp() {
		assertBlockValidationResult(123, TEST_HEIGHT + 1, 123, ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsSuccessIfBlockContainsTransactionsWithDeadlineLaterThanBlockTimeStamp() {
		assertBlockValidationResult(123, TEST_HEIGHT + 1, 124, ValidationResult.SUCCESS);
	}

	private static void assertBlockValidationResult(final int blockTimeStamp, final long blockHeight, final int transactionDeadline,
			final ValidationResult expectedResult) {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithTimeStampAndHeight(blockTimeStamp, blockHeight);
		for (int i = 0; i < 5; i++) {
			// add some not expired transactions
			addTransaction(block, blockTimeStamp + 10);
		}

		addTransaction(block, transactionDeadline);

		// Assert:
		MatcherAssert.assertThat(VALIDATOR.validate(block), IsEqual.equalTo(expectedResult));
	}

	private static void addTransaction(final Block block, final int deadline) {
		final Transaction transaction = new MockTransaction();
		transaction.setDeadline(new TimeInstant(deadline));
		block.addTransaction(transaction);
	}
}
