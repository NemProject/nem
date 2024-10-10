package org.nem.nis.validators.unconfirmed;

import java.util.*;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.DefaultHashCache;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

public class BatchUniqueHashTransactionValidatorTest {
	@Test
	public void validateReturnsNeutralWhenAtLeastOneHashAlreadyExistsInHashCache() {
		// Arrange:
		final TestContext context = new TestContext(5);
		final ValidationContext validationContext = new ValidationContext(new BlockHeight(10), new BlockHeight(17), ValidationStates.Throw);
		final List<TransactionsContextPair> groupedTransactions = new ArrayList<>();
		groupedTransactions
				.add(new TransactionsContextPair(Arrays.asList(new MockTransaction(Utils.generateRandomAccount(), 7)), validationContext));
		groupedTransactions.add(new TransactionsContextPair(
				Arrays.asList(new MockTransaction(Utils.generateRandomAccount(), 5), context.transactions.get(3)), validationContext));
		groupedTransactions
				.add(new TransactionsContextPair(Arrays.asList(new MockTransaction(Utils.generateRandomAccount(), 3)), validationContext));

		// Act:
		final ValidationResult result = context.validator.validate(groupedTransactions);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void validateReturnsNeutralWhenAtLeastOneChildHashAlreadyExistsInHashCache() {
		// Arrange:
		final TestContext context = new TestContext(5);
		final ValidationContext validationContext = new ValidationContext(new BlockHeight(10), new BlockHeight(17), ValidationStates.Throw);

		final MockTransaction parentTransactionWithAlreadySeenChild = new MockTransaction(Utils.generateRandomAccount(), 10);
		parentTransactionWithAlreadySeenChild.setChildTransactions(
				Arrays.asList(TransactionExtensions.streamDefault(context.transactions.get(3)).collect(Collectors.toList()).get(1)));

		final List<TransactionsContextPair> groupedTransactions = new ArrayList<>();
		groupedTransactions
				.add(new TransactionsContextPair(Arrays.asList(new MockTransaction(Utils.generateRandomAccount(), 7)), validationContext));
		groupedTransactions.add(new TransactionsContextPair(
				Arrays.asList(new MockTransaction(Utils.generateRandomAccount(), 5), parentTransactionWithAlreadySeenChild),
				validationContext));
		groupedTransactions
				.add(new TransactionsContextPair(Arrays.asList(new MockTransaction(Utils.generateRandomAccount(), 3)), validationContext));

		// Act:
		final ValidationResult result = context.validator.validate(groupedTransactions);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void validateReturnsSuccessWhenNoneOfTheHashesExistInHashCache() {
		// Arrange:
		final TestContext context = new TestContext(5);
		final ValidationContext validationContext = new ValidationContext(new BlockHeight(10), new BlockHeight(17), ValidationStates.Throw);
		final List<TransactionsContextPair> groupedTransactions = new ArrayList<>();
		groupedTransactions
				.add(new TransactionsContextPair(Arrays.asList(new MockTransaction(Utils.generateRandomAccount(), 7)), validationContext));
		groupedTransactions.add(new TransactionsContextPair(
				Arrays.asList(new MockTransaction(Utils.generateRandomAccount(), 5), new MockTransaction(Utils.generateRandomAccount(), 9)),
				validationContext));
		groupedTransactions
				.add(new TransactionsContextPair(Arrays.asList(new MockTransaction(Utils.generateRandomAccount(), 3)), validationContext));

		// Act:
		final ValidationResult result = context.validator.validate(groupedTransactions);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsSuccessWhenCalledWithEmptyGroups() {
		// Arrange:
		final TestContext context = new TestContext(5);
		final List<TransactionsContextPair> groupedTransactions = new ArrayList<>();

		// Act:
		final ValidationResult result = context.validator.validate(groupedTransactions);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	private static class TestContext {
		private final List<Transaction> transactions = new ArrayList<>();
		private final DefaultHashCache transactionHashCache = new DefaultHashCache();
		private final BatchUniqueHashTransactionValidator validator = new BatchUniqueHashTransactionValidator(this.transactionHashCache);

		public TestContext(final int count) {
			final HashMetaData defaultMetaData = new HashMetaData(new BlockHeight(1), new TimeInstant(0));
			for (int i = 0; i < count; ++i) {
				final Transaction transaction = MockTransactionUtils.createMockTransactionWithNestedChildren(i);
				this.transactions.add(transaction);
				TransactionExtensions.streamDefault(transaction).map(HashUtils::calculateHash)
						.forEach(hash -> this.transactionHashCache.put(new HashMetaDataPair(hash, defaultMetaData)));
			}
		}
	}
}
