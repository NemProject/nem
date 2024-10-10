package org.nem.nis.validators.block;

import java.util.*;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.DefaultHashCache;

public class BlockUniqueHashTransactionValidatorTest {
	@Test
	public void validateReturnsNeutralWhenAtLeastOneHashAlreadyExistsInHashCache() {
		// Arrange:
		final TestContext context = new TestContext(5);
		final List<Transaction> blockTransactions = new ArrayList<>();
		blockTransactions.add(new MockTransaction(Utils.generateRandomAccount(), 7));
		blockTransactions.add(context.transactions.get(3)); // root transaction
		blockTransactions.add(new MockTransaction(Utils.generateRandomAccount(), 9));
		context.setBlockTransactions(blockTransactions);

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void validateReturnsNeutralWhenAtLeastOneChildHashAlreadyExistsInHashCache() {
		// Arrange:
		final TestContext context = new TestContext(5);

		// - add mock transaction with child transaction already in cache
		final List<Transaction> blockTransactions = new ArrayList<>();
		final MockTransaction parentTransactionWithAlreadySeenChild = new MockTransaction(Utils.generateRandomAccount(), 10);
		parentTransactionWithAlreadySeenChild.setChildTransactions(
				Arrays.asList(TransactionExtensions.streamDefault(context.transactions.get(3)).collect(Collectors.toList()).get(1)));

		blockTransactions.add(new MockTransaction(Utils.generateRandomAccount(), 7));
		blockTransactions.add(parentTransactionWithAlreadySeenChild);
		blockTransactions.add(new MockTransaction(Utils.generateRandomAccount(), 9));
		context.setBlockTransactions(blockTransactions);

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void validateReturnsSuccessWhenNoneOfTheHashesExistInHashCache() {
		// Arrange:
		final TestContext context = new TestContext(5);
		final List<Transaction> blockTransactions = new ArrayList<>();
		blockTransactions.add(new MockTransaction(Utils.generateRandomAccount(), 7));
		blockTransactions.add(new MockTransaction(Utils.generateRandomAccount(), 8));
		blockTransactions.add(new MockTransaction(Utils.generateRandomAccount(), 9));
		context.setBlockTransactions(blockTransactions);

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsSuccessWhenCalledWithEmptyBlock() {
		// Arrange:
		final TestContext context = new TestContext(5);
		final List<Transaction> blockTransactions = new ArrayList<>();
		context.setBlockTransactions(blockTransactions);

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	private static class TestContext {
		private final Block block = Mockito.mock(Block.class);
		private final List<Transaction> transactions = new ArrayList<>();
		private final DefaultHashCache transactionHashCache = new DefaultHashCache();
		private final BlockUniqueHashTransactionValidator validator = new BlockUniqueHashTransactionValidator(this.transactionHashCache);

		private TestContext(final int count) {
			final HashMetaData defaultMetaData = new HashMetaData(new BlockHeight(1), new TimeInstant(0));
			for (int i = 0; i < count; ++i) {
				final Transaction transaction = MockTransactionUtils.createMockTransactionWithNestedChildren(i);
				this.transactions.add(transaction);
				TransactionExtensions.streamDefault(transaction).map(HashUtils::calculateHash)
						.forEach(hash -> this.transactionHashCache.put(new HashMetaDataPair(hash, defaultMetaData)));
			}
		}

		private void setBlockTransactions(final List<Transaction> transactions) {
			Mockito.when(this.block.getTransactions()).thenReturn(transactions);
		}
	}
}
