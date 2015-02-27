package org.nem.nis.chain;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.NisCacheFactory;

import java.util.*;

@RunWith(Enclosed.class)
public class BlockExecutorTest {

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
		protected void process(final Transaction transaction, final Block block, final ReadOnlyNisCache nisCache, final BlockTransactionObserver observer) {
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
		protected void process(final Transaction transaction, final Block block, final ReadOnlyNisCache nisCache, final BlockTransactionObserver observer) {
			final BlockExecutor executor = new BlockExecutor(nisCache);
			executor.undo(block, observer);
		}
	}

	public static class LegacyTests {

		//region execute / undo basic transaction order

		@Test
		public void executeCallsExecuteOnAllTransactionsInForwardOrder() {
			// Arrange:
			final UndoExecuteTransactionOrderTestContext context = new UndoExecuteTransactionOrderTestContext();

			// Act:
			context.execute();

			// Assert:
			Assert.assertThat(context.executeList, IsEqual.equalTo(Arrays.asList(1, 2, 3)));
		}

		@Test
		public void undoCallsUndoOnAllTransactionsInReverseOrder() {
			// Arrange:
			final UndoExecuteTransactionOrderTestContext context = new UndoExecuteTransactionOrderTestContext();

			// Act:
			context.execute();
			context.undo();

			// Assert:
			Assert.assertThat(context.undoList, IsEqual.equalTo(Arrays.asList(3, 2, 1)));
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
				this.transactions = new MockTransaction[] {
						this.createTransaction(1, 17),
						this.createTransaction(2, 11),
						this.createTransaction(3, 4)
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
					transaction.setTransactionAction(o -> this.executeList.add(transaction.getCustomField()));
				}

				this.executor.execute(this.block, Mockito.mock(BlockTransactionObserver.class));
			}

			private void undo() {
				for (final MockTransaction transaction : this.transactions) {
					transaction.setTransactionAction(o -> this.undoList.add(transaction.getCustomField()));
				}

				this.executor.undo(this.block, Mockito.mock(BlockTransactionObserver.class));
			}

			private MockTransaction createTransaction(final int customField, final long fee) {
				return BlockUtils.createTransactionWithFee(customField, fee);
			}
		}

		//endregion

		//region execute / undo CHILD transaction hashes notifications

		@Test
		public void executePropagatesChildTransactionHashesToSubscribedObserver() {
			// Arrange:
			final UndoExecuteChildTransactionHashesTestContext context = new UndoExecuteChildTransactionHashesTestContext();
			final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

			// Act:
			final TransactionHashesNotification notification = context.execute(observer);

			// check notification
			Assert.assertThat(notification.getPairs().size(), IsEqual.equalTo(3));
			NotificationUtils.assertTransactionHashesNotification(notification, context.transactionHashPairs);
		}

		@Test
		public void undoPropagatesChildTransactionHashesToSubscribedObserver() {
			// Arrange:
			final UndoExecuteChildTransactionHashesTestContext context = new UndoExecuteChildTransactionHashesTestContext();
			final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

			// Act:
			final TransactionHashesNotification notification = context.undo(observer);

			// check notification
			Assert.assertThat(notification.getPairs().size(), IsEqual.equalTo(3));
			NotificationUtils.assertTransactionHashesNotification(notification, context.transactionHashPairs);
		}

		private static class UndoExecuteChildTransactionHashesTestContext {
			private final ExecutorTestContext context = new ExecutorTestContext();
			private final Account signer = this.context.addAccount();
			private final Account transactionSigner = this.context.addAccount();

			private final BlockHeight height = new BlockHeight(11);
			private final Block block;

			private final List<HashMetaDataPair> transactionHashPairs = new ArrayList<>();

			public UndoExecuteChildTransactionHashesTestContext() {
				// Arrange: create a block with two child transactions
				this.block = new Block(this.signer, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, this.height);

				final MockTransaction childTransaction1 = new MockTransaction(Utils.generateRandomAccount(), 2);
				final MockTransaction childTransaction2 = new MockTransaction(Utils.generateRandomAccount(), 4);

				final MockTransaction transaction = new MockTransaction(this.transactionSigner, 1);
				transaction.setChildTransactions(Arrays.asList(childTransaction1, childTransaction2));
				this.block.addTransaction(transaction);

				final HashMetaData metaData = new HashMetaData(this.height, MockTransaction.TIMESTAMP);
				this.transactionHashPairs.add(new HashMetaDataPair(HashUtils.calculateHash(childTransaction1), metaData));
				this.transactionHashPairs.add(new HashMetaDataPair(HashUtils.calculateHash(childTransaction2), metaData));
				this.transactionHashPairs.add(new HashMetaDataPair(HashUtils.calculateHash(transaction), metaData));
			}

			private TransactionHashesNotification execute(final BlockTransactionObserver observer) {
				this.context.executor.execute(this.block, observer);

				final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
				Mockito.verify(observer, Mockito.times(4)).notify(notificationCaptor.capture(), Mockito.any());
				return (TransactionHashesNotification)notificationCaptor.getAllValues().get(3);
			}

			private TransactionHashesNotification undo(final BlockTransactionObserver observer) {
				this.context.executor.undo(this.block, observer);

				final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
				Mockito.verify(observer, Mockito.times(4)).notify(notificationCaptor.capture(), Mockito.any());
				return (TransactionHashesNotification)notificationCaptor.getAllValues().get(2);
			}
		}

		//endregion

		private static class ExecutorTestContext {
			private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
			private final AccountCache accountCache = Mockito.mock(AccountCache.class);
			private final NisCache nisCache = NisCacheFactory.create(this.accountCache, this.accountStateCache);
			private final BlockExecutor executor = new BlockExecutor(this.nisCache);

			private Account addAccount() {
				final Account account = Utils.generateRandomAccount();
				this.hookAccount(account);
				return account;
			}

			private Block createBlockWithTransaction(final BlockHeight height, final Transaction transaction) {
				final Block block = BlockUtils.createBlockWithHeight(height);
				this.hookAccount(block.getSigner());
				block.addTransaction(transaction);
				return block;
			}

			private void hookAccount(final Account account) {
				final AccountState accountState = new AccountState(account.getAddress());
				Mockito.when(this.accountStateCache.findForwardedStateByAddress(Mockito.eq(account.getAddress()), Mockito.any()))
						.thenReturn(accountState);
			}

			private void setForwardingAccount(final Account forwardingAccount, final Account forwardAccount) {
				Mockito.when(this.accountCache.findByAddress(forwardAccount.getAddress())).thenReturn(forwardAccount);

				final AccountState accountState = new AccountState(forwardAccount.getAddress());
				Mockito.when(this.accountStateCache.findForwardedStateByAddress(Mockito.eq(forwardingAccount.getAddress()), Mockito.any()))
						.thenReturn(accountState);
			}
		}
	}
}