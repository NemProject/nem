package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.dao.*;

import java.util.*;
import java.util.function.Consumer;

public class BatchUniqueHashTransactionValidatorTest {

	//region some transaction hash already exists in transfer dao

	@Test
	public void validateReturnsNeutralIfAtLeastOneHashAlreadyExistsInTransferDao() {
		// Assert:
		assertNeutralIfTransactionAlreadyExistsInDao(TestContext::setTransferDaoForHashes);
	}

	@Test
	public void validateReturnsNeutralIfAtLeastOneHashAlreadyExistsInImportanceTransferDao() {
		// Assert:
		assertNeutralIfTransactionAlreadyExistsInDao(TestContext::setImportanceTransferDaoForHashes);
	}

	private static void assertNeutralIfTransactionAlreadyExistsInDao(final Consumer<TestContext> setTransactionInDao) {
		// Arrange:
		final TestContext context = new TestContext();
		setTransactionInDao.accept(context);

		// Act:
		final ValidationResult result = context.validateAtHeight(10);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	//endregion

	@Test
	public void validateReturnsSuccessIfCalledWithEmptyList() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validator.validate(Arrays.asList());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsSuccessIfNoneOfTheHashesExistInAnyDao() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validateAtHeight(10);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	private static class TestContext {
		private final BlockHeight confirmedBlockHeight = new BlockHeight(17);
		private final List<Transaction> transactions = new ArrayList<>();
		private final List<Hash> hashes = new ArrayList<>();

		private final TransferDao transferDao = Mockito.mock(TransferDao.class);
		private final ImportanceTransferDao importanceTransferDao = Mockito.mock(ImportanceTransferDao.class);
		private final BatchUniqueHashTransactionValidator validator = new BatchUniqueHashTransactionValidator(this.transferDao, this.importanceTransferDao);

		public TestContext() {
			for (int i = 0; i < 5; ++i) {
				final Transaction transaction = new MockTransaction(Utils.generateRandomAccount(), 7 + i);
				this.transactions.add(transaction);
				this.hashes.add(HashUtils.calculateHash(transaction));
			}
		}

		private void setTransferDaoForHashes() {
			Mockito.when(this.transferDao.anyHashExists(this.hashes, this.confirmedBlockHeight))
					.thenReturn(true);
		}

		private void setImportanceTransferDaoForHashes() {
			Mockito.when(this.importanceTransferDao.anyHashExists(this.hashes, this.confirmedBlockHeight))
					.thenReturn(true);
		}

		private ValidationResult validateAtHeight(final long height) {
			final List<TransactionsContextPair> groupedTransactions = new ArrayList<>();
			groupedTransactions.add(this.createPair(0, 1, height));
			groupedTransactions.add(this.createPair(2, 4, height + 1));
			return this.validator.validate(groupedTransactions);
		}

		private TransactionsContextPair createPair(final int start, final int end, final long height) {
			final ValidationContext validationContext = new ValidationContext(new BlockHeight(height), this.confirmedBlockHeight);
			final List<Transaction> transactions = new ArrayList<>();
			for (int i = start; i <= end; ++i) {
				transactions.add(this.transactions.get(i));
			}

			return new TransactionsContextPair(transactions, validationContext);
		}
	}
}
