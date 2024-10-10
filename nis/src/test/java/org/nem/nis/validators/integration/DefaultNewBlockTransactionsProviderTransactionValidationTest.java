package org.nem.nis.validators.integration;

import java.util.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.ForkConfiguration;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.harvesting.*;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.test.*;

public class DefaultNewBlockTransactionsProviderTransactionValidationTest extends AbstractTransactionValidationTest {

	@Override
	protected void assertTransactions(final BlockHeight chainHeight, final ReadOnlyNisCache nisCache, final List<Transaction> all,
			final List<Transaction> expectedFiltered, final ValidationResult expectedResult) {
		final TestContext context = new TestContext(chainHeight, nisCache);
		context.addTransactions(all);

		// Act:
		final List<Transaction> blockTransactions = context.getBlockTransactions();

		// Assert:
		MatcherAssert.assertThat(blockTransactions.size(), IsEqual.equalTo(expectedFiltered.size()));
		MatcherAssert.assertThat(blockTransactions, IsEquivalent.equivalentTo(expectedFiltered));
	}

	private static class TestContext {
		private final UnconfirmedTransactions transactions;
		private final NewBlockTransactionsProvider provider;

		private TestContext(final BlockHeight chainHeight, final ReadOnlyNisCache nisCache) {
			final int maxTransactionsPerBlock = NisTestConstants.MAX_TRANSACTIONS_PER_BLOCK;
			final ForkConfiguration forkConfiguration = new ForkConfiguration.Builder().build();
			final UnconfirmedStateFactory unconfirmedStateFactory = new UnconfirmedStateFactory(
					NisUtils.createTransactionValidatorFactory(),
					NisUtils.createBlockTransactionObserverFactory()::createExecuteCommitObserver,
					Utils.createMockTimeProvider(CURRENT_TIME.getRawTime()), () -> chainHeight, maxTransactionsPerBlock, forkConfiguration);
			this.transactions = new DefaultUnconfirmedTransactions(unconfirmedStateFactory, nisCache);

			this.provider = new DefaultNewBlockTransactionsProvider(nisCache, NisUtils.createTransactionValidatorFactory(),
					NisUtils.createBlockValidatorFactory(), new BlockTransactionObserverFactory(forkConfiguration),
					this.transactions.asFilter(), forkConfiguration);
		}

		public List<Transaction> getBlockTransactions() {
			return this.provider.getBlockTransactions(Utils.generateRandomAccount().getAddress(), CURRENT_TIME.addSeconds(5),
					new BlockHeight(511000));
		}

		public void addTransactions(final Collection<? extends Transaction> transactions) {
			transactions.forEach(this.transactions::addNew);
		}
	}
}
