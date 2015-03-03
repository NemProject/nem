package org.nem.nis.test.validation;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.harvesting.*;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.test.NisUtils;

import java.util.*;

public class DefaultNewBlockTransactionsProviderTransactionValidationTest extends AbstractTransactionValidationTest {

	@Override
	protected void assertTransactions(
			final ReadOnlyNisCache nisCache,
			final List<Transaction> all,
			final List<Transaction> expectedFiltered,
			final ValidationResult expectedResult) {
		final TestContext context = new TestContext(nisCache);
		context.addTransactions(all);

		// Act:
		final List<Transaction> blockTransactions = context.getBlockTransactions();

		// Assert:
		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(expectedFiltered.size()));
		Assert.assertThat(blockTransactions, IsEquivalent.equivalentTo(expectedFiltered));
	}

	private static class TestContext {
		private final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
		private final List<Transaction> transactions = new ArrayList<>();
		private final NewBlockTransactionsProvider provider;

		private TestContext(final ReadOnlyNisCache nisCache) {
			Mockito.when(this.unconfirmedTransactions.getTransactionsBefore(Mockito.any()))
					.thenReturn(this.transactions);

			this.provider = new DefaultNewBlockTransactionsProvider(
					nisCache,
					NisUtils.createTransactionValidatorFactory(),
					NisUtils.createBlockValidatorFactory(),
					new BlockTransactionObserverFactory(),
					this.unconfirmedTransactions);
		}

		public List<Transaction> getBlockTransactions() {
			return this.provider.getBlockTransactions(
					Utils.generateRandomAccount().getAddress(),
					TimeInstant.ZERO,
					new BlockHeight(10));
		}

		public void addTransactions(final Collection<? extends Transaction> transactions) {
			this.transactions.addAll(transactions);
		}
	}
}
