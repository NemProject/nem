package org.nem.nis.secret;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.function.Function;

public class BlockTransactionObserverFactoryTest {

	//region basic

	@Test
	public void createExecuteCommitObserverReturnsValidObserver() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		// Act:
		final BlockTransactionObserver observer = factory.createExecuteCommitObserver(context.nisCache);

		// Assert:
		Assert.assertThat(observer, IsNull.notNullValue());
	}

	@Test
	public void createUndoCommitObserverReturnsValidObserver() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		// Act:
		final BlockTransactionObserver observer = factory.createUndoCommitObserver(context.nisCache);

		// Assert:
		Assert.assertThat(observer, IsNull.notNullValue());
	}

	//endregion

	//region outlink side effects

	@Test
	public void executeUpdatesOutlinks() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createExecuteCommitObserver(context.nisCache);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext1.account, context.accountContext2.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.accountContext1.importance, Mockito.times(1)).addOutlink(Mockito.any());
		Mockito.verify(context.accountContext2.importance, Mockito.times(0)).addOutlink(Mockito.any());
	}

	@Test
	public void undoUpdatesOutlinks() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createUndoCommitObserver(context.nisCache);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext2.account, context.accountContext1.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(context.accountContext1.importance, Mockito.times(1)).removeOutlink(Mockito.any());
		Mockito.verify(context.accountContext2.importance, Mockito.times(0)).removeOutlink(Mockito.any());
	}

	//endregion

	//region weighted balance side effects

	@Test
	public void executeUpdatesWeightedBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createExecuteCommitObserver(context.nisCache);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext1.account, context.accountContext2.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));
		observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceDebit, context.accountContext1.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.accountContext1.balances, Mockito.times(2)).addSend(Mockito.any(), Mockito.any());
		Mockito.verify(context.accountContext2.balances, Mockito.times(1)).addReceive(Mockito.any(), Mockito.any());
	}

	@Test
	public void undoUpdatesWeightedBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createUndoCommitObserver(context.nisCache);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext2.account, context.accountContext1.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Undo));
		observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, context.accountContext1.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(context.accountContext1.balances, Mockito.times(2)).undoSend(Mockito.any(), Mockito.any());
		Mockito.verify(context.accountContext2.balances, Mockito.times(1)).undoReceive(Mockito.any(), Mockito.any());
	}

	//endregion

	//region execute / undo outlink update ordering

	@Test
	public void executeAddsOutlinkAfterUpdatingBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<String> breadcrumbs = new ArrayList<>();
		final Function<String, Answer> createAnswer = breadcrumb -> invocationOnMock -> {
			breadcrumbs.add(breadcrumb);
			return null;
		};

		Mockito.doAnswer(createAnswer.apply("outlink")).when(context.accountContext1.importance).addOutlink(Mockito.any());
		Mockito.doAnswer(createAnswer.apply("balance")).when(context.accountContext1.accountInfo).decrementBalance(Mockito.any());
		Mockito.doAnswer(createAnswer.apply("weighted-balance")).when(context.accountContext1.balances).addSend(Mockito.any(), Mockito.any());

		// Act:
		final BlockTransactionObserver observer = context.factory.createExecuteCommitObserver(context.nisCache);
		observer.notify(
				new BalanceTransferNotification(context.accountContext1.account, context.accountContext2.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Assert.assertThat(breadcrumbs, IsEqual.equalTo(Arrays.asList("weighted-balance", "balance", "outlink")));
	}

	@Test
	public void undoRemovesOutlinkBeforeUpdatingBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<String> breadcrumbs = new ArrayList<>();
		final Function<String, Answer> createAnswer = breadcrumb -> invocationOnMock -> {
			breadcrumbs.add(breadcrumb);
			return null;
		};

		Mockito.doAnswer(createAnswer.apply("outlink")).when(context.accountContext1.importance).removeOutlink(Mockito.any());
		Mockito.doAnswer(createAnswer.apply("balance")).when(context.accountContext1.accountInfo).incrementBalance(Mockito.any());
		Mockito.doAnswer(createAnswer.apply("weighted-balance")).when(context.accountContext1.balances).undoSend(Mockito.any(), Mockito.any());

		// Act:
		final BlockTransactionObserver observer = context.factory.createUndoCommitObserver(context.nisCache);
		observer.notify(
				new BalanceTransferNotification(context.accountContext2.account, context.accountContext1.account, Amount.fromNem(1)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Undo));

		// Assert:
		Assert.assertThat(breadcrumbs, IsEqual.equalTo(Arrays.asList("outlink", "balance", "weighted-balance")));
	}

	//endregion

	private static class MockAccountContext {
		private final Account account = Mockito.mock(Account.class);
		private final AccountInfo accountInfo = Mockito.mock(AccountInfo.class);
		private final AccountImportance importance = Mockito.mock(AccountImportance.class);
		private final WeightedBalances balances = Mockito.mock(WeightedBalances.class);
		private final Address address = Utils.generateRandomAddress();

		public MockAccountContext(final PoiFacade poiFacade) {
			Mockito.when(this.account.getAddress()).thenReturn(this.address);

			final AccountState accountState = Mockito.mock(AccountState.class);
			Mockito.when(accountState.getAccountInfo()).thenReturn(this.accountInfo);
			Mockito.when(accountState.getWeightedBalances()).thenReturn(this.balances);
			Mockito.when(accountState.getImportanceInfo()).thenReturn(this.importance);

			Mockito.when(poiFacade.findStateByAddress(this.address)).thenReturn(accountState);
		}
	}

	private static class TestContext {
		private final DefaultPoiFacade poiFacade = Mockito.mock(DefaultPoiFacade.class);
		private final MockAccountContext accountContext1 = this.addAccount();
		private final MockAccountContext accountContext2 = this.addAccount();
		private final NisCache nisCache = NisUtils.createNisCache(this.poiFacade);
		private final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		private MockAccountContext addAccount() {
			return new MockAccountContext(this.poiFacade);
		}
	}
}