package org.nem.nis.validators.block;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.cache.DefaultHashCache;

import java.util.*;

public class BlockUniqueHashTransactionValidatorTest {

	@Test
	public void validateReturnsNeutralIfAtLeastOneHashAlreadyExistsInHashCache() {
		// Assert:
		assertValidationResult(ValidationResult.NEUTRAL, 5, true);
	}

	@Test
	public void validateReturnsSuccessIfNoneOfTheHashesExistInHashCache() {
		// Assert:
		assertValidationResult(ValidationResult.SUCCESS, 5, false);
	}

	@Test
	public void validateReturnsSuccessIfCalledWithEmptyList() {
		// Assert:
		assertValidationResult(ValidationResult.SUCCESS, 0, true);
	}

	private static void assertValidationResult(final ValidationResult expectedResult, final int numTransactions,
			final boolean anyHashExistsReturnValue) {
		// Arrange:
		final TestContext context = new TestContext(numTransactions);
		Mockito.when(context.transactionHashCache.anyHashExists(context.hashes)).thenReturn(anyHashExistsReturnValue);

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static class TestContext {
		private final Block block = Mockito.mock(Block.class);
		private final List<Transaction> transactions = new ArrayList<>();
		private final List<Hash> hashes = new ArrayList<>();
		private final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
		private final BlockUniqueHashTransactionValidator validator = new BlockUniqueHashTransactionValidator(this.transactionHashCache);

		private TestContext(final int count) {
			this.addTransactions(count);
			Mockito.when(this.block.getTransactions()).thenReturn(this.transactions);
		}

		private void addTransactions(final int count) {
			for (int i = 0; i < count; ++i) {
				final Transaction transaction = new MockTransaction(Utils.generateRandomAccount(), i);
				this.transactions.add(transaction);
				this.hashes.add(HashUtils.calculateHash(transaction));
			}
		}
	}
}
