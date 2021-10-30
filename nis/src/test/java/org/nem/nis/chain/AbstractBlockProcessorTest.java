package org.nem.nis.chain;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
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

public abstract class AbstractBlockProcessorTest {
	// if transaction execution is supported, transaction (4) and block (3) notifications will be raised separately
	// if not, they will be raised together
	private final int numExpectedTransactionNotifications = this.supportsTransactionExecution() ? 4 : 7;
	private final int numExpectedBlockNotifications = this.supportsTransactionExecution() ? 3 : 7;

	// region abstract methods

	protected abstract NotificationTrigger getTrigger();

	protected abstract boolean supportsTransactionExecution();

	protected abstract void process(final Block block, final ReadOnlyNisCache nisCache, final BlockTransactionObserver observer);

	protected abstract void process(final Transaction transaction, final Block block, final ReadOnlyNisCache nisCache,
			final BlockTransactionObserver observer);

	// endregion

	// region notification propagation

	@Test
	public void processPropagatesAllTransactionNotificationsToSubscribedObserver() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();

		// Act:
		this.processTransaction(context);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(this.numExpectedTransactionNotifications);

		// check notifications
		final List<Notification> values = notificationCaptor.getAllValues();
		if (NotificationTrigger.Execute == this.getTrigger()) {
			NotificationUtils.assertBalanceDebitNotification(values.get(3), context.account1, Amount.fromNem(11));
			NotificationUtils.assertBalanceCreditNotification(values.get(2), context.account2, Amount.fromNem(2));
			NotificationUtils.assertBalanceCreditNotification(values.get(1), context.account1, Amount.fromNem(9));
			NotificationUtils.assertBalanceTransferNotification(values.get(0), context.account1, context.account2, Amount.fromNem(12));
		} else {
			// transaction notifications should be undone last
			final int start = this.numExpectedTransactionNotifications - 4;
			NotificationUtils.assertBalanceCreditNotification(values.get(start), context.account1, Amount.fromNem(11));
			NotificationUtils.assertBalanceDebitNotification(values.get(start + 1), context.account2, Amount.fromNem(2));
			NotificationUtils.assertBalanceDebitNotification(values.get(start + 2), context.account1, Amount.fromNem(9));
			NotificationUtils.assertBalanceTransferNotification(values.get(start + 3), context.account2, context.account1,
					Amount.fromNem(12));
		}
	}

	@Test
	public void processPropagatesHarvestNotificationsToSubscribedObserver() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();

		// Act:
		this.processBlock(context);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(this.numExpectedBlockNotifications);

		// check notifications
		final List<Notification> values = notificationCaptor.getAllValues();
		if (NotificationTrigger.Execute == this.getTrigger()) {
			// block harvest notifications should be executed second to last
			final int start = this.numExpectedBlockNotifications - 2;
			NotificationUtils.assertBlockHarvestNotification(values.get(start), context.block.getSigner(), Amount.fromNem(7));
			NotificationUtils.assertBalanceCreditNotification(values.get(start - 1), context.block.getSigner(), Amount.fromNem(7));
		} else {
			// block harvest notifications should be undone second
			NotificationUtils.assertBlockHarvestNotification(values.get(1), context.block.getSigner(), Amount.fromNem(7));
			NotificationUtils.assertBalanceDebitNotification(values.get(2), context.block.getSigner(), Amount.fromNem(7));
		}
	}

	@Test
	public void processPropagatesTransactionHashesNotificationToSubscribedObserver() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();

		// Act:
		this.processBlock(context);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(this.numExpectedBlockNotifications);

		// check notification
		final Notification notification = this.getTransactionHashesNotification(notificationCaptor.getAllValues(),
				this.numExpectedBlockNotifications);
		NotificationUtils.assertTransactionHashesNotification(notification, context.transactionHashPairs);
	}

	@Test
	public void processPropagatesProperNotificationContextWithAllTransactionNotifications() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();

		// Act:
		this.processTransaction(context);

		// Assert:
		final ArgumentCaptor<BlockNotificationContext> notificationContextCaptor = context
				.captureNotificationContexts(this.numExpectedTransactionNotifications);

		// check notification contexts
		for (final BlockNotificationContext notificationContext : notificationContextCaptor.getAllValues()) {
			MatcherAssert.assertThat(notificationContext.getHeight(), IsEqual.equalTo(context.height));
			MatcherAssert.assertThat(notificationContext.getTrigger(), IsEqual.equalTo(this.getTrigger()));
		}
	}

	@Test
	public void processPropagatesProperNotificationContextWithAllBlockNotifications() {
		// Arrange:
		final UndoExecuteNotificationTestContext context = new UndoExecuteNotificationTestContext();

		// Act:
		this.processBlock(context);

		// Assert:
		final ArgumentCaptor<BlockNotificationContext> notificationContextCaptor = context
				.captureNotificationContexts(this.numExpectedBlockNotifications);

		// check notification contexts
		for (final BlockNotificationContext notificationContext : notificationContextCaptor.getAllValues()) {
			MatcherAssert.assertThat(notificationContext.getHeight(), IsEqual.equalTo(context.height));
			MatcherAssert.assertThat(notificationContext.getTrigger(), IsEqual.equalTo(this.getTrigger()));
		}
	}

	private static class UndoExecuteNotificationTestContext extends ProcessorTestContext {
		// Arrange:
		private final Account account1 = this.addAccount();
		private final Account account2 = this.addAccount();
		private final List<HashMetaDataPair> transactionHashPairs;

		public UndoExecuteNotificationTestContext() {
			final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
			transaction.setFee(Amount.fromNem(7));
			transaction.setTransferAction(to -> {
				to.notify(new BalanceTransferNotification(this.account1, this.account2, Amount.fromNem(12)));
				to.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, this.account1, Amount.fromNem(9)));
				to.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, this.account2, Amount.fromNem(2)));
				to.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.account1, Amount.fromNem(11)));
			});

			this.block = this.createBlockWithTransaction(this.height, transaction);
			final HashMetaDataPair pair = new HashMetaDataPair(HashUtils.calculateHash(transaction),
					new HashMetaData(this.height, transaction.getTimeStamp()));
			this.transactionHashPairs = Collections.singletonList(pair);
		}
	}

	// endregion

	// region REMOTE harvest notifications

	@Test
	public void processPropagatesHarvestNotificationsFromRemoteAsEndowedToSubscribedObserver() {
		// Arrange:
		final Account realAccount = Utils.generateRandomAccount();
		final Account remoteSigner = Utils.generateRandomAccount();
		final ProcessorTestContext context = createTestContextForRemoteHarvestingTests(realAccount, remoteSigner);

		// Act:
		this.processBlock(context);

		// Assert:
		final int expectedNotifications = (this.supportsTransactionExecution() ? 0 : 1) + 3;
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(expectedNotifications);
		final List<Notification> values = notificationCaptor.getAllValues();

		// check notifications - all harvest related notifications should contain the forwarded account (realAccount)
		if (NotificationTrigger.Execute == this.getTrigger()) {
			final int start = expectedNotifications - 2;
			NotificationUtils.assertBlockHarvestNotification(values.get(start), realAccount, Amount.fromNem(5));
			NotificationUtils.assertBalanceCreditNotification(values.get(start - 1), realAccount, Amount.fromNem(5));
		} else {
			NotificationUtils.assertBlockHarvestNotification(values.get(1), realAccount, Amount.fromNem(5));
			NotificationUtils.assertBalanceDebitNotification(values.get(2), realAccount, Amount.fromNem(5));
		}
	}

	@Test
	public void processorDelegatesStateLookupToAccountStateCache() {
		// Arrange:
		final Account realAccount = Utils.generateRandomAccount();
		final Account remoteSigner = Utils.generateRandomAccount();
		final ProcessorTestContext context = createTestContextForRemoteHarvestingTests(realAccount, remoteSigner);

		// Act:
		this.processBlock(context);

		// Assert:
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findForwardedStateByAddress(remoteSigner.getAddress(), context.height);
	}

	@Test
	public void processorDelegatesAccountLookupToAccountCache() {
		// Arrange:
		final Account realAccount = Utils.generateRandomAccount();
		final Account remoteSigner = Utils.generateRandomAccount();
		final ProcessorTestContext context = createTestContextForRemoteHarvestingTests(realAccount, remoteSigner);

		// Act:
		this.processBlock(context);

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(1)).findByAddress(realAccount.getAddress());
	}

	private static ProcessorTestContext createTestContextForRemoteHarvestingTests(final Account realAccount, final Account remoteSigner) {
		// Arrange:
		final ProcessorTestContext context = new ProcessorTestContext();
		context.hookAccount(realAccount);
		context.hookAccount(remoteSigner);
		final Account transactionSigner = context.addAccount();

		// Arrange: create a block signed by the remote (remoteSigner) and have remoteSigner forward to realAccount
		context.block = new Block(remoteSigner, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, context.height);
		final MockTransaction transaction = new MockTransaction(transactionSigner, 1);
		transaction.setFee(Amount.fromNem(5));
		context.block.addTransaction(transaction);
		context.setForwardingAccount(remoteSigner, realAccount);
		return context;
	}

	// endregion

	// region CHILD transaction hashes notifications

	@Test
	public void processPropagatesChildTransactionHashesToSubscribedObserver() {
		// Arrange:
		final ProcessorTestContext context = new ProcessorTestContext();
		final Account signer = context.addAccount();
		final Account transactionSigner = context.addAccount();

		// Arrange: create a block with two child transactions
		context.block = new Block(signer, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, context.height);

		final MockTransaction childTransaction1 = new MockTransaction(Utils.generateRandomAccount(), 2);
		final MockTransaction childTransaction2 = new MockTransaction(Utils.generateRandomAccount(), 4);

		final MockTransaction transaction = new MockTransaction(transactionSigner, 1);
		transaction.setChildTransactions(Arrays.asList(childTransaction1, childTransaction2));
		context.block.addTransaction(transaction);

		final HashMetaData metaData = new HashMetaData(context.height, MockTransaction.TIMESTAMP);
		final List<HashMetaDataPair> transactionHashPairs = new ArrayList<>();
		transactionHashPairs.add(new HashMetaDataPair(HashUtils.calculateHash(childTransaction1), metaData));
		transactionHashPairs.add(new HashMetaDataPair(HashUtils.calculateHash(childTransaction2), metaData));
		transactionHashPairs.add(new HashMetaDataPair(HashUtils.calculateHash(transaction), metaData));

		// Act:
		this.processBlock(context);

		// Assert:
		final int expectedNotifications = (this.supportsTransactionExecution() ? 0 : 1) + 3;
		final ArgumentCaptor<Notification> notificationCaptor = context.captureNotifications(expectedNotifications);

		// check notification
		final TransactionHashesNotification notification = this.getTransactionHashesNotification(notificationCaptor.getAllValues(),
				expectedNotifications);
		MatcherAssert.assertThat(notification.getPairs().size(), IsEqual.equalTo(3));
		NotificationUtils.assertTransactionHashesNotification(notification, transactionHashPairs);
	}

	// endregion

	// region helpers

	private TransactionHashesNotification getTransactionHashesNotification(final List<Notification> notifications,
			final int expectedNotifications) {
		// when executed, transaction hashes are notified last; when undone they are first
		final int start = NotificationTrigger.Execute == this.getTrigger() ? expectedNotifications - 1 : 0;
		return (TransactionHashesNotification) notifications.get(start);
	}

	private void processTransaction(final ProcessorTestContext context) {
		this.process(context.block.getTransactions().get(0), context.block, context.nisCache, context.observer);
	}

	private void processBlock(final ProcessorTestContext context) {
		this.process(context.block, context.nisCache, context.observer);
	}

	private static class ProcessorTestContext {
		public final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		public final AccountCache accountCache = Mockito.mock(AccountCache.class);
		public final NisCache nisCache = NisCacheFactory.create(this.accountCache, this.accountStateCache);

		public final BlockHeight height = new BlockHeight(11);
		public Block block;
		public final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);

		public Account addAccount() {
			final Account account = Utils.generateRandomAccount();
			this.hookAccount(account);
			return account;
		}

		public Block createBlockWithTransaction(final BlockHeight height, final Transaction transaction) {
			final Block block = BlockUtils.createBlockWithHeight(height);
			this.hookAccount(block.getSigner());
			block.addTransaction(transaction);
			return block;
		}

		public void hookAccount(final Account account) {
			final AccountState accountState = new AccountState(account.getAddress());
			Mockito.when(this.accountStateCache.findForwardedStateByAddress(Mockito.eq(account.getAddress()), Mockito.any()))
					.thenReturn(accountState);
		}

		public void setForwardingAccount(final Account forwardingAccount, final Account forwardAccount) {
			Mockito.when(this.accountCache.findByAddress(forwardAccount.getAddress())).thenReturn(forwardAccount);

			final AccountState accountState = new AccountState(forwardAccount.getAddress());
			Mockito.when(this.accountStateCache.findForwardedStateByAddress(Mockito.eq(forwardingAccount.getAddress()), Mockito.any()))
					.thenReturn(accountState);
		}

		public ArgumentCaptor<Notification> captureNotifications(final int numExpectedNotifications) {
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(this.observer, Mockito.times(numExpectedNotifications)).notify(notificationCaptor.capture(), Mockito.any());
			return notificationCaptor;
		}

		public ArgumentCaptor<BlockNotificationContext> captureNotificationContexts(final int numExpectedNotifications) {
			final ArgumentCaptor<BlockNotificationContext> contextCaptor = ArgumentCaptor.forClass(BlockNotificationContext.class);
			Mockito.verify(this.observer, Mockito.times(numExpectedNotifications)).notify(Mockito.any(), contextCaptor.capture());
			return contextCaptor;
		}
	}

	// endregion
}
