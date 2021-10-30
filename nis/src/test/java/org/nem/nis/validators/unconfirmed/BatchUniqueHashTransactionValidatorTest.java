package org.nem.nis.validators.unconfirmed;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.cache.DefaultHashCache;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.function.Consumer;

public class BatchUniqueHashTransactionValidatorTest {

	// region some transaction hash already exists in cache

	@Test
	public void validateReturnsNeutralIfAtLeastOneHashAlreadyExistsInHashCache() {
		// Assert:
		assertNeutralIfTransactionAlreadyExistsInHashCache(TestContext::setTransactionHashCacheForHashes);
	}

	private static void assertNeutralIfTransactionAlreadyExistsInHashCache(final Consumer<TestContext> setTransactionHashCacheForHashes) {
		// Arrange:
		final TestContext context = new TestContext();
		setTransactionHashCacheForHashes.accept(context);

		// Act:
		final ValidationResult result = context.validateAtHeight(10);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	// endregion

	@Test
	public void validateReturnsSuccessIfCalledWithEmptyList() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validator.validate(Collections.emptyList());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsSuccessIfNoneOfTheHashesExistInHashCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validateAtHeight(10);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	private static class TestContext {
		private final BlockHeight confirmedBlockHeight = new BlockHeight(17);
		private final List<Transaction> transactions = new ArrayList<>();
		private final List<Hash> hashes = new ArrayList<>();

		private final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
		private final BatchUniqueHashTransactionValidator validator = new BatchUniqueHashTransactionValidator(this.transactionHashCache);

		public TestContext() {
			for (int i = 0; i < 5; ++i) {
				final Transaction transaction = new MockTransaction(Utils.generateRandomAccount(), 7 + i);
				this.transactions.add(transaction);
				this.hashes.add(HashUtils.calculateHash(transaction));
			}
		}

		private void setTransactionHashCacheForHashes() {
			Mockito.when(this.transactionHashCache.anyHashExists(this.hashes)).thenReturn(true);
		}

		private ValidationResult validateAtHeight(final long height) {
			final List<TransactionsContextPair> groupedTransactions = new ArrayList<>();
			groupedTransactions.add(this.createPair(0, 1, height));
			groupedTransactions.add(this.createPair(2, 4, height + 1));
			return this.validator.validate(groupedTransactions);
		}

		private TransactionsContextPair createPair(final int start, final int end, final long height) {
			final ValidationContext validationContext = new ValidationContext(new BlockHeight(height), this.confirmedBlockHeight,
					ValidationStates.Throw);
			final List<Transaction> transactions = new ArrayList<>();
			for (int i = start; i <= end; ++i) {
				transactions.add(this.transactions.get(i));
			}

			return new TransactionsContextPair(transactions, validationContext);
		}
	}
}
