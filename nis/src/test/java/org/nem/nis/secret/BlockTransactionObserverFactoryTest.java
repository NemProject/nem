package org.nem.nis.secret;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.poi.*;

import java.util.*;
import java.util.function.Function;

public class BlockTransactionObserverFactoryTest {

	//region basic

	@Test
	public void createExecuteCommitObserverReturnsValidObserver() {
		// Arrange:
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		// Act:
		final BlockTransactionObserver observer = factory.createExecuteCommitObserver(Mockito.mock(AccountAnalyzer.class));

		// Assert:
		Assert.assertThat(observer, IsNull.notNullValue());
	}

	@Test
	public void createUndoCommitObserverReturnsValidObserver() {
		// Arrange:
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		// Act:
		final BlockTransactionObserver observer = factory.createUndoCommitObserver(Mockito.mock(AccountAnalyzer.class));

		// Assert:
		Assert.assertThat(observer, IsNull.notNullValue());
	}

	//endregion

	//region outlink side effects

	@Test
	public void executeUpdatesOutlinks() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createExecuteCommitObserver(context.accountAnalyzer);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext1.account, context.accountContext2.account, Amount.fromNem(1)),
				new BlockNotificationContext(new BlockHeight(11), new TimeInstant(123), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.accountContext1.importance, Mockito.times(1)).addOutlink(Mockito.any());
		Mockito.verify(context.accountContext2.importance, Mockito.times(0)).addOutlink(Mockito.any());
	}

	@Test
	public void undoUpdatesOutlinks() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createUndoCommitObserver(context.accountAnalyzer);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext2.account, context.accountContext1.account, Amount.fromNem(1)),
				new BlockNotificationContext(new BlockHeight(11), new TimeInstant(123), NotificationTrigger.Undo));

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
		final BlockTransactionObserver observer = context.factory.createExecuteCommitObserver(context.accountAnalyzer);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext1.account, context.accountContext2.account, Amount.fromNem(1)),
				new BlockNotificationContext(new BlockHeight(11), new TimeInstant(123), NotificationTrigger.Execute));
		observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceDebit, context.accountContext1.account, Amount.fromNem(1)),
				new BlockNotificationContext(new BlockHeight(11), new TimeInstant(123), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.accountContext1.balances, Mockito.times(2)).addSend(Mockito.any(), Mockito.any());
		Mockito.verify(context.accountContext2.balances, Mockito.times(1)).addReceive(Mockito.any(), Mockito.any());
	}

	@Test
	public void undoUpdatesWeightedBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.factory.createUndoCommitObserver(context.accountAnalyzer);

		// Act:
		observer.notify(
				new BalanceTransferNotification(context.accountContext2.account, context.accountContext1.account, Amount.fromNem(1)),
				new BlockNotificationContext(new BlockHeight(11), new TimeInstant(123), NotificationTrigger.Undo));
		observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, context.accountContext1.account, Amount.fromNem(1)),
				new BlockNotificationContext(new BlockHeight(11), new TimeInstant(123), NotificationTrigger.Undo));

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
		Mockito.doAnswer(createAnswer.apply("balance")).when(context.accountContext1.account).decrementBalance(Mockito.any());
		Mockito.doAnswer(createAnswer.apply("weighted-balance")).when(context.accountContext1.balances).addSend(Mockito.any(), Mockito.any());

		// Act:
		final BlockTransactionObserver observer = context.factory.createExecuteCommitObserver(context.accountAnalyzer);
		observer.notify(
				new BalanceTransferNotification(context.accountContext1.account, context.accountContext2.account, Amount.fromNem(1)),
				new BlockNotificationContext(new BlockHeight(11), new TimeInstant(123), NotificationTrigger.Execute));

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
		Mockito.doAnswer(createAnswer.apply("balance")).when(context.accountContext1.account).incrementBalance(Mockito.any());
		Mockito.doAnswer(createAnswer.apply("weighted-balance")).when(context.accountContext1.balances).undoSend(Mockito.any(), Mockito.any());

		// Act:
		final BlockTransactionObserver observer = context.factory.createUndoCommitObserver(context.accountAnalyzer);
		observer.notify(
				new BalanceTransferNotification(context.accountContext2.account, context.accountContext1.account, Amount.fromNem(1)),
				new BlockNotificationContext(new BlockHeight(11), new TimeInstant(123), NotificationTrigger.Undo));

		// Assert:
		Assert.assertThat(breadcrumbs, IsEqual.equalTo(Arrays.asList("outlink", "balance", "weighted-balance")));
	}

	//endregion

	private static class MockAccountContext {
		private final Account account;
		private final AccountImportance importance;
		private final WeightedBalances balances;
		private final Address address;

		public MockAccountContext(final PoiFacade poiFacade) {
			this.account = Mockito.mock(Account.class);
			this.importance = Mockito.mock(AccountImportance.class);
			this.balances = Mockito.mock(WeightedBalances.class);
			this.address = Utils.generateRandomAddress();

			Mockito.when(this.account.getAddress()).thenReturn(this.address);

			final PoiAccountState accountState = Mockito.mock(PoiAccountState.class);
			Mockito.when(accountState.getWeightedBalances()).thenReturn(this.balances);
			Mockito.when(accountState.getImportanceInfo()).thenReturn(this.importance);

			Mockito.when(poiFacade.findStateByAddress(this.address)).thenReturn(accountState);
		}
	}

	private static class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final MockAccountContext accountContext1 = this.addAccount();
		private final MockAccountContext accountContext2 = this.addAccount();
		private final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(new AccountCache(), this.poiFacade);
		private final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		private MockAccountContext addAccount() {
			return new MockAccountContext(this.poiFacade);
		}
	}
}