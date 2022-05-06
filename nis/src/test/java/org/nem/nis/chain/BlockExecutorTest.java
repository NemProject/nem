package org.nem.nis.chain;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.NisCacheFactory;

import java.util.*;

@RunWith(Enclosed.class)
public class BlockExecutorTest {

	// region ExecutorAsExecuteProcessorTests / ExecutorAsUndoProcessorTests

	public static class ExecutorAsExecuteProcessorTests extends AbstractBlockProcessorTest {

		@Override
		protected NotificationTrigger getTrigger() {
			return NotificationTrigger.Execute;
		}

		@Override
		protected boolean supportsTransactionExecution() {
			return false;
		}

		@Override
		protected void process(final Block block, final ReadOnlyNisCache nisCache, final BlockTransactionObserver observer) {
			final BlockExecutor executor = new BlockExecutor(nisCache);
			executor.execute(block, observer);
		}

		@Override
		protected void process(final Transaction transaction, final Block block, final ReadOnlyNisCache nisCache,
				final BlockTransactionObserver observer) {
			final BlockExecutor executor = new BlockExecutor(nisCache);
			executor.execute(block, observer);
		}
	}

	public static class ExecutorAsUndoProcessorTests extends AbstractBlockProcessorTest {

		@Override
		protected NotificationTrigger getTrigger() {
			return NotificationTrigger.Undo;
		}

		@Override
		protected boolean supportsTransactionExecution() {
			return false;
		}

		@Override
		protected void process(final Block block, final ReadOnlyNisCache nisCache, final BlockTransactionObserver observer) {
			final BlockExecutor executor = new BlockExecutor(nisCache);
			executor.undo(block, observer);
		}

		@Override
		protected void process(final Transaction transaction, final Block block, final ReadOnlyNisCache nisCache,
				final BlockTransactionObserver observer) {
			final BlockExecutor executor = new BlockExecutor(nisCache);
			executor.undo(block, observer);
		}
	}

	// endregion

	public static class UndoExecuteTransactionOrderTests {

		@Test
		public void executeCallsExecuteOnAllTransactionsInForwardOrder() {
			// Arrange:
			final UndoExecuteTransactionOrderTestContext context = new UndoExecuteTransactionOrderTestContext();

			// Act:
			context.execute();

			// Assert:
			MatcherAssert.assertThat(context.executeList, IsEqual.equalTo(Arrays.asList(1, 2, 3)));
		}

		@Test
		public void undoCallsUndoOnAllTransactionsInReverseOrder() {
			// Arrange:
			final UndoExecuteTransactionOrderTestContext context = new UndoExecuteTransactionOrderTestContext();

			// Act:
			context.execute();
			context.undo();

			// Assert:
			MatcherAssert.assertThat(context.undoList, IsEqual.equalTo(Arrays.asList(3, 2, 1)));
		}

		private final class UndoExecuteTransactionOrderTestContext {
			private final Account account;
			private final Block block;
			private final MockTransaction[] transactions;
			private final List<Integer> executeList = new ArrayList<>();
			private final List<Integer> undoList = new ArrayList<>();
			private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
			private final NisCache nisCache = NisCacheFactory.create(this.accountStateCache);
			private final BlockExecutor executor = new BlockExecutor(this.nisCache);

			public UndoExecuteTransactionOrderTestContext() {
				this.account = Utils.generateRandomAccount();
				this.transactions = new MockTransaction[]{
						this.createTransaction(1, 17), this.createTransaction(2, 11), this.createTransaction(3, 4)
				};

				this.block = BlockUtils.createBlock(this.account);
				for (final MockTransaction transaction : this.transactions) {
					this.block.addTransaction(transaction);
				}

				final AccountState accountState = new AccountState(this.account.getAddress());
				accountState.getWeightedBalances().addReceive(BlockHeight.ONE, new Amount(100));
				Mockito.when(this.accountStateCache.findForwardedStateByAddress(this.account.getAddress(), this.block.getHeight()))
						.thenReturn(accountState);
			}

			private void execute() {
				for (final MockTransaction transaction : this.transactions) {
					transaction.setTransferAction(o -> this.executeList.add(transaction.getCustomField()));
				}

				this.executor.execute(this.block, Mockito.mock(BlockTransactionObserver.class));
			}

			private void undo() {
				for (final MockTransaction transaction : this.transactions) {
					transaction.setTransferAction(o -> this.undoList.add(transaction.getCustomField()));
				}

				this.executor.undo(this.block, Mockito.mock(BlockTransactionObserver.class));
			}

			private MockTransaction createTransaction(final int customField, final long fee) {
				return BlockUtils.createTransactionWithFee(customField, fee);
			}
		}
	}
}
