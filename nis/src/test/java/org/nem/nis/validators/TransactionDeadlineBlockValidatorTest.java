package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.MockTransaction;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.NisUtils;

public class TransactionDeadlineBlockValidatorTest {
	private static final long BETA_TX_DEADLINE_FORK = BlockMarkerConstants.BETA_TX_DEADLINE_FORK;
	private static final TransactionDeadlineBlockValidator VALIDATOR = new TransactionDeadlineBlockValidator();

	@Test
	public void validateReturnsSuccessIfBlockContainsTransactionWithDeadlineEarlierThanBlockTimeStampBeforeBetaTxDeadlineFork() {
		assertBlockValidationResult(123, BETA_TX_DEADLINE_FORK - 1, 122, ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsFailureIfBlockContainsTransactionWithDeadlineEarlierThanBlockTimeStampAtBetaTxDeadlineFork() {
		assertBlockValidationResult(123, BETA_TX_DEADLINE_FORK, 122, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	@Test
	public void validateReturnsFailureIfBlockContainsTransactionWithDeadlineEarlierThanBlockTimeStampAfterBetaTxDeadlineFork() {
		assertBlockValidationResult(123, BETA_TX_DEADLINE_FORK + 1, 122, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	@Test
	public void validateReturnsSuccessIfBlockContainsTransactionWithDeadlineEqualToBlockTimeStamp() {
		assertBlockValidationResult(123, BETA_TX_DEADLINE_FORK + 1, 123, ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsSuccessIfBlockContainsTransactionsWithDeadlineLaterThanBlockTimeStamp() {
		assertBlockValidationResult(123, BETA_TX_DEADLINE_FORK + 1, 124, ValidationResult.SUCCESS);
	}

	private void assertBlockValidationResult(
			final int blockTimeStamp,
			final long blockHeight,
			final int transactionDeadline,
			final ValidationResult expectedResult) {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithTimeStampAndHeight(blockTimeStamp, blockHeight);
		for (int i=0; i<5; i++) {
			// add some not expired transactions
			addTransaction(block, blockTimeStamp + 10);
		}

		addTransaction(block, transactionDeadline);

		// Assert:
		Assert.assertThat(VALIDATOR.validate(block), IsEqual.equalTo(expectedResult));
	}

	private void addTransaction(final Block block, final int deadline) {
		final Transaction transaction = new MockTransaction();
		transaction.setDeadline(new TimeInstant(deadline));
		block.addTransaction(transaction);
	}
}
