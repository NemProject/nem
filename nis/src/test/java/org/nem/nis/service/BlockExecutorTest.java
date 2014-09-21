package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.observers.Notification;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.poi.*;
import org.nem.nis.secret.*;

import java.util.*;

public class BlockExecutorTest {

	//region execute / undo basic updates

	@Test
	public void executeCallsExecuteOnAllTransactionsInForwardOrder() {
		// Arrange:
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.execute();

		// Assert:
		Assert.assertThat(context.executeList, IsEqual.equalTo(Arrays.asList(1, 2, 3)));
	}

	@Test
	public void undoCallsUndoOnAllTransactionsInReverseOrder() {
		// Arrange:
		final UndoExecuteTestContext context = new UndoExecuteTestContext();

		// Act:
		context.execute();
		context.undo();

		// Assert:
		Assert.assertThat(context.undoList, IsEqual.equalTo(Arrays.asList(3, 2, 1)));
	}

	private final class UndoExecuteTestContext {
		private final Account account;
		private final Block block;
		private final MockTransaction[] transactions;
		private final List<Integer> executeList = new ArrayList<>();
		private final List<Integer> undoList = new ArrayList<>();
		private final BlockExecutor executor = createBlockExecutor();

		public UndoExecuteTestContext() {
			this.account = Utils.generateRandomAccount();
			this.account.incrementBalance(new Amount(100));
			for (int i = 0; i < 3; ++i) {
				this.account.incrementForagedBlocks();
			}

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

	//region execute / undo notifications

	@Test
	public void executePropagatesAllNotificationsDelegatesToSubscribedObserver() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount().account;
		final Account account2 = context.addAccount().account;

		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setFee(Amount.fromNem(7));
		transaction.setTransferAction(to -> {
			to.notifyTransfer(account1, account2, Amount.fromNem(12));
			to.notifyCredit(account1, Amount.fromNem(9));
			to.notifyDebit(account1, Amount.fromNem(11));
		});

		final BlockHeight height = new BlockHeight(11);
		final Block block = context.createBlockWithTransaction(height, Amount.fromNem(7), transaction);

		// Act:
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		context.executor.execute(block, observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		final ArgumentCaptor<BlockNotificationContext> notificationContextCaptor = ArgumentCaptor.forClass(BlockNotificationContext.class);
		Mockito.verify(observer, Mockito.times(6)).notify(notificationCaptor.capture(), notificationContextCaptor.capture());

		// check notifications
		NotificationUtils.assertHarvestRewardNotification(notificationCaptor.getAllValues().get(5), block.getSigner(), Amount.fromNem(7));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(4), account1, Amount.fromNem(11));
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(3), account1, Amount.fromNem(9));
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(2), account1);
		NotificationUtils.assertBalanceTransferNotification(notificationCaptor.getAllValues().get(1), account1, account2, Amount.fromNem(12));
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), account2);

		// check notification contexts
		for (final BlockNotificationContext notificationContext : notificationContextCaptor.getAllValues()) {
			Assert.assertThat(notificationContext.getHeight(), IsEqual.equalTo(height));
			Assert.assertThat(notificationContext.getTrigger(), IsEqual.equalTo(NotificationTrigger.Execute));
		}
	}

	@Test
	public void undoPropagatesAllNotificationsToSubscribedObserver() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount().account;
		final Account account2 = context.addAccount().account;

		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setFee(Amount.fromNem(7));
		transaction.setTransferAction(to -> {
			to.notifyTransfer(account1, account2, Amount.fromNem(12));
			to.notifyCredit(account1, Amount.fromNem(9));
			to.notifyDebit(account1, Amount.fromNem(11));
		});

		final BlockHeight height = new BlockHeight(11);
		final Block block = context.createBlockWithTransaction(height, Amount.fromNem(7), transaction);

		// Act:
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		context.executor.execute(block, Mockito.mock(BlockTransactionObserver.class));
		context.executor.undo(block, observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		final ArgumentCaptor<BlockNotificationContext> notificationContextCaptor = ArgumentCaptor.forClass(BlockNotificationContext.class);
		Mockito.verify(observer, Mockito.times(6)).notify(notificationCaptor.capture(), notificationContextCaptor.capture());

		// check notifications
		NotificationUtils.assertHarvestRewardNotification(notificationCaptor.getAllValues().get(0), block.getSigner(), Amount.fromNem(7));
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(1), account1, Amount.fromNem(11));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(2), account1, Amount.fromNem(9));
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(3), account1);
		NotificationUtils.assertBalanceTransferNotification(notificationCaptor.getAllValues().get(4), account2, account1, Amount.fromNem(12));
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(5), account2);

		// check notification contexts
		for (final BlockNotificationContext notificationContext : notificationContextCaptor.getAllValues()) {
			Assert.assertThat(notificationContext.getHeight(), IsEqual.equalTo(height));
			Assert.assertThat(notificationContext.getTrigger(), IsEqual.equalTo(NotificationTrigger.Undo));
		}
	}

	//endregion

	private static class MockAccountContext {
		private final Account account;
		private final Address address;

		public MockAccountContext() {
			this.account = Mockito.mock(Account.class);
			this.address = Utils.generateRandomAddress();
			Mockito.when(this.account.getAddress()).thenReturn(this.address);
		}
	}

	private static class TestContext {
		private final BlockExecutor executor = createBlockExecutor();

		private MockAccountContext addAccount() {
			return new MockAccountContext();
		}

		private Block createBlockWithTransaction(final BlockHeight height, final Amount amount, final Transaction transaction) {
			final Block block = BlockUtils.createBlockWithHeight(height);
			block.getSigner().incrementBalance(amount);
			block.addTransaction(transaction);
			return block;
		}
	}

	private static BlockExecutor createBlockExecutor() {
		return new BlockExecutor();
	}
}