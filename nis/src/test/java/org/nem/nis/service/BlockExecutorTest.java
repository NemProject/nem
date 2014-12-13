package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.poi.*;
import org.nem.nis.secret.*;

import java.util.*;

public class BlockExecutorTest {

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
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final NisCache nisCache = new NisCache(Mockito.mock(AccountCache.class), this.poiFacade, Mockito.mock(HashCache.class));
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

			final PoiAccountState accountState = new PoiAccountState(this.account.getAddress());
			accountState.getWeightedBalances().addReceive(BlockHeight.ONE, new Amount(100));
			Mockito.when(this.poiFacade.findForwardedStateByAddress(this.account.getAddress(), this.block.getHeight()))
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

	//region execute / undo notification propagation

	@Test
	public void executePropagatesAllTransactionNotificationsToSubscribedObserver() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		// Act:
		context.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(observer);

		// check notifications
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(2), context.account1, Amount.fromNem(11));
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(1), context.account1, Amount.fromNem(9));
		NotificationUtils.assertBalanceTransferNotification(notificationCaptor.getAllValues().get(0), context.account1, context.account2, Amount.fromNem(12));
	}

	@Test
	public void executePropagatesHarvestNotificationsToSubscribedObserver() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		// Act:
		context.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(observer);

		// check notifications
		NotificationUtils.assertHarvestRewardNotification(notificationCaptor.getAllValues().get(4), context.block.getSigner(), Amount.fromNem(7));
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(3), context.block.getSigner(), Amount.fromNem(7));
	}

	@Test
	public void executePropagatesTransactionHashesNotificationToSubscribedObserver() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		// Act:
		context.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(observer);

		// check notification
		NotificationUtils.assertTransactionHashesNotification(notificationCaptor.getAllValues().get(5), context.transactionHashPairs);
	}

	@Test
	public void executePropagatesProperNotificationContextWithAllNotifications() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		// Act:
		context.execute(observer);

		// Assert:
		final ArgumentCaptor<BlockNotificationContext> notificationContextCaptor = context.captureNotificationContexts(observer);

		// check notification contexts
		for (final BlockNotificationContext notificationContext : notificationContextCaptor.getAllValues()) {
			Assert.assertThat(notificationContext.getHeight(), IsEqual.equalTo(context.height));
			Assert.assertThat(notificationContext.getTrigger(), IsEqual.equalTo(NotificationTrigger.Execute));
		}
	}

	@Test
	public void undoPropagatesAllTransactionNotificationsToSubscribedObserver() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		// Act:
		context.execute(Mockito.mock(BlockTransactionObserver.class));
		context.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(observer);

		// check notifications
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(3), context.account1, Amount.fromNem(11));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(4), context.account1, Amount.fromNem(9));
		NotificationUtils.assertBalanceTransferNotification(notificationCaptor.getAllValues().get(5), context.account2, context.account1, Amount.fromNem(12));
	}

	@Test
	public void undoPropagatesHarvestNotificationsToSubscribedObserver() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		// Act:
		context.execute(Mockito.mock(BlockTransactionObserver.class));
		context.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(observer);

		// check notifications
		NotificationUtils.assertHarvestRewardNotification(notificationCaptor.getAllValues().get(0), context.block.getSigner(), Amount.fromNem(7));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(1), context.block.getSigner(), Amount.fromNem(7));
	}

	@Test
	public void undoPropagatesTransactionHashesNotificationToSubscribedObserver() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		// Act:
		context.execute(Mockito.mock(BlockTransactionObserver.class));
		context.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(observer);

		// check notifications
		NotificationUtils.assertTransactionHashesNotification(notificationCaptor.getAllValues().get(2), context.transactionHashPairs);
	}

	@Test
	public void undoPropagatesProperNotificationContextWithAllNotifications() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		// Act:
		context.execute(Mockito.mock(BlockTransactionObserver.class));
		context.undo(observer);

		// Assert:
		final ArgumentCaptor<BlockNotificationContext> notificationContextCaptor = context.captureNotificationContexts(observer);

		// check notification contexts
		for (final BlockNotificationContext notificationContext : notificationContextCaptor.getAllValues()) {
			Assert.assertThat(notificationContext.getHeight(), IsEqual.equalTo(context.height));
			Assert.assertThat(notificationContext.getTrigger(), IsEqual.equalTo(NotificationTrigger.Undo));
		}
	}

	private static class UndoExecuteNotificationTestContext {
		// Arrange:
		private final ExecutorTestContext context = new ExecutorTestContext();
		private final Account account1 = this.context.addAccount();
		private final Account account2 = this.context.addAccount();
		private final BlockHeight height = new BlockHeight(11);
		private final Block block;
		private final List<HashMetaDataPair> transactionHashPairs;

		public UndoExecuteNotificationTestContext() {
			final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
			transaction.setFee(Amount.fromNem(7));
			transaction.setTransactionAction(to -> {
				to.notify(new BalanceTransferNotification(this.account1, this.account2, Amount.fromNem(12)));
				to.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, this.account1, Amount.fromNem(9)));
				to.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.account1, Amount.fromNem(11)));
			});

			this.block = this.context.createBlockWithTransaction(this.height, transaction);
			final HashMetaDataPair pair = new HashMetaDataPair(
					HashUtils.calculateHash(transaction),
					new HashMetaData(this.height, transaction.getTimeStamp()));
			this.transactionHashPairs = Arrays.asList(pair);
		}

		private void execute(final BlockTransactionObserver observer) {
			this.context.executor.execute(this.block, observer);
		}

		private void undo(final BlockTransactionObserver observer) {
			this.context.executor.undo(this.block, observer);
		}

		private ArgumentCaptor<Notification> captureNotifications(final BlockTransactionObserver observer) {
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(6)).notify(notificationCaptor.capture(), Mockito.any());
			return notificationCaptor;
		}

		private ArgumentCaptor<BlockNotificationContext> captureNotificationContexts(final BlockTransactionObserver observer) {
			final ArgumentCaptor<BlockNotificationContext> contextCaptor = ArgumentCaptor.forClass(BlockNotificationContext.class);
			Mockito.verify(observer, Mockito.times(6)).notify(Mockito.any(), contextCaptor.capture());
			return contextCaptor;
		}
	}

	//endregion

	//region execute / undo remote harvest notifications

	@Test
	public void executePropagatesHarvestNotificationsFromRemoteAsEndowedToSubscribedObserver() {
		// Arrange:
		final UndoExecuteRemoteHarvestingNotificationTestContext context = new UndoExecuteRemoteHarvestingNotificationTestContext();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		// Act:
		context.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(4)).notify(notificationCaptor.capture(), Mockito.any());

		// check notifications - all harvest related notifications should contain the forwarded account (realAccount)
		NotificationUtils.assertHarvestRewardNotification(notificationCaptor.getAllValues().get(2), context.realAccount, Amount.fromNem(5));
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(1), context.realAccount, Amount.fromNem(5));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(0), context.transactionSigner, Amount.fromNem(5));
	}

	@Test
	public void undoPropagatesHarvestNotificationsFromRemoteAsEndowedToSubscribedObserver() {
		// Arrange:
		final UndoExecuteRemoteHarvestingNotificationTestContext context = new UndoExecuteRemoteHarvestingNotificationTestContext();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		// Act:
		context.execute(Mockito.mock(BlockTransactionObserver.class));
		context.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(4)).notify(notificationCaptor.capture(), Mockito.any());

		// check notifications - all harvest related notifications should contain the forwarded account (realAccount)
		NotificationUtils.assertHarvestRewardNotification(notificationCaptor.getAllValues().get(0), context.realAccount, Amount.fromNem(5));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(1), context.realAccount, Amount.fromNem(5));
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(3), context.transactionSigner, Amount.fromNem(5));
	}

	private static class UndoExecuteRemoteHarvestingNotificationTestContext {
		private final ExecutorTestContext context = new ExecutorTestContext();
		private final Account remoteSigner = this.context.addAccount();
		private final Account realAccount = this.context.addAccount();
		private final Account transactionSigner = this.context.addAccount();

		final BlockHeight height = new BlockHeight(11);
		final Block block;

		public UndoExecuteRemoteHarvestingNotificationTestContext() {
			// Arrange: create a block signed by the remote (remoteSigner) and have remoteSigner forward to realAccount
			this.block = new Block(this.remoteSigner, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, this.height);
			final MockTransaction transaction = new MockTransaction(this.transactionSigner, 1);
			transaction.setMinimumFee(Amount.fromNem(5).getNumMicroNem());
			this.block.addTransaction(transaction);

			this.context.setForwardingAccount(this.remoteSigner, this.realAccount);
		}

		private void execute(final BlockTransactionObserver observer) {
			this.context.executor.execute(this.block, observer);
		}

		private void undo(final BlockTransactionObserver observer) {
			this.context.executor.undo(this.block, observer);
		}
	}

	//endregion

	//region delegation

	@Test
	public void executorDelegatesStateLookupToPoiFacade() {
		// Arrange:
		final UndoExecuteRemoteHarvestingNotificationTestContext context = new UndoExecuteRemoteHarvestingNotificationTestContext();

		// Act:
		context.execute(Mockito.mock(BlockTransactionObserver.class));

		// Assert:
		Mockito.verify(context.context.poiFacade, Mockito.times(1))
				.findForwardedStateByAddress(context.remoteSigner.getAddress(), context.height);
	}

	@Test
	public void executorDelegatesAccountLookupToAccountCache() {
		// Arrange:
		final UndoExecuteRemoteHarvestingNotificationTestContext context = new UndoExecuteRemoteHarvestingNotificationTestContext();

		// Act:
		context.execute(Mockito.mock(BlockTransactionObserver.class));

		// Assert:
		Mockito.verify(context.context.accountCache, Mockito.times(1)).findByAddress(context.realAccount.getAddress());
	}

	//endregion

	private static class ExecutorTestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final NisCache nisCache = new NisCache(this.accountCache, this.poiFacade, Mockito.mock(HashCache.class));
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
			final PoiAccountState accountState = new PoiAccountState(account.getAddress());
			Mockito.when(this.poiFacade.findForwardedStateByAddress(Mockito.eq(account.getAddress()), Mockito.any()))
					.thenReturn(accountState);
		}

		private void setForwardingAccount(final Account forwardingAccount, final Account forwardAccount) {
			Mockito.when(this.accountCache.findByAddress(forwardAccount.getAddress())).thenReturn(forwardAccount);

			final PoiAccountState accountState = new PoiAccountState(forwardAccount.getAddress());
			Mockito.when(this.poiFacade.findForwardedStateByAddress(Mockito.eq(forwardingAccount.getAddress()), Mockito.any()))
					.thenReturn(accountState);
		}
	}
}