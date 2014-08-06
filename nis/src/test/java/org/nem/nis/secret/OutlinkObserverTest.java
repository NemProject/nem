package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

public class OutlinkObserverTest {

	//region notifyTransfer

	@Test
	public void notifyTransferExecuteAddsSenderOutlink() {
		// Arrange:
		final TestContext context = new TestContext();
		final OutlinkObserver observer = new OutlinkObserver(new BlockHeight(111), true);

		// Act:
		observer.notifyTransfer(context.account1, context.account2, new Amount(752));

		// Assert (752 * 0.3 = 225.5):
		final AccountLink expectedLink = new AccountLink(new BlockHeight(111), new Amount(225), context.account2.getAddress());
		Mockito.verify(context.importance1, Mockito.times(1)).addOutlink(expectedLink);
		verifyCallCounts(context.importance1, 1, 0);
		verifyCallCounts(context.importance2, 0, 0);
	}

	@Test
	public void notifyTransferUndoRemovesRecipientOutlink() {
		// Arrange:
		final TestContext context = new TestContext();
		final OutlinkObserver observer = new OutlinkObserver(new BlockHeight(111), false);

		// Act:
		observer.notifyTransfer(context.account2, context.account1, new Amount(752));

		// Assert (752 * 0.3 = 225.5):
		final AccountLink expectedLink = new AccountLink(new BlockHeight(111), new Amount(225), context.account2.getAddress());
		Mockito.verify(context.importance1, Mockito.times(1)).removeOutlink(expectedLink);
		verifyCallCounts(context.importance1, 0, 1);
		verifyCallCounts(context.importance2, 0, 0);
	}

	@Test
	public void notifyTransferDoesNotAddSelfOutlink() {
		// Arrange:
		final TestContext context = new TestContext();
		final OutlinkObserver observer = new OutlinkObserver(new BlockHeight(111), true);

		// Act:
		observer.notifyTransfer(context.account1, context.account1, new Amount(752));

		// Assert:
		verifyCallCounts(context.importance1, 0, 0);
		verifyCallCounts(context.importance2, 0, 0);
	}

	//endregion

	//region notifyCredit

	@Test
	public void notifyCreditExecuteDoesNotChangeOutlinks() {
		// Assert:
		assertNotifyCreditDoesNotChangeOutlinks(true);
	}

	@Test
	public void notifyCreditUndoDoesNotChangeOutlinks() {
		// Assert:
		assertNotifyCreditDoesNotChangeOutlinks(false);
	}

	private static void assertNotifyCreditDoesNotChangeOutlinks(boolean isExecute) {
		// Arrange:
		final TestContext context = new TestContext();
		final OutlinkObserver observer = new OutlinkObserver(new BlockHeight(111), isExecute);

		// Act:
		observer.notifyCredit(context.account1, new Amount(432));

		// Assert:
		verifyCallCounts(context.importance1, 0, 0);
	}

	//endregion

	//region notifyDebit

	@Test
	public void notifyDebitExecuteDoesNotChangeOutlinks() {
		// Assert:
		assertNotifyDebitDoesNotChangeOutlinks(true);
	}

	@Test
	public void notifyDebitUndoDoesNotChangeOutlinks() {
		// Assert:
		assertNotifyDebitDoesNotChangeOutlinks(false);
	}

	private static void assertNotifyDebitDoesNotChangeOutlinks(boolean isExecute) {
		// Arrange:
		final TestContext context = new TestContext();
		final OutlinkObserver observer = new OutlinkObserver(new BlockHeight(111), isExecute);

		// Act:
		observer.notifyDebit(context.account1, new Amount(432));

		// Assert:
		verifyCallCounts(context.importance1, 0, 0);
	}

	//endregion

	private static void verifyCallCounts(
			final AccountImportance importance,
			final int addOutlinkCounts,
			final int removeOutlinkCounts) {
		Mockito.verify(importance, Mockito.times(addOutlinkCounts)).addOutlink(Mockito.any());
		Mockito.verify(importance, Mockito.times(removeOutlinkCounts)).removeOutlink(Mockito.any());
	}

	private static class TestContext {
		private final Account account1;
		private final Account account2;
		private final AccountImportance importance1;
		private final AccountImportance importance2;
		private final WeightedBalances weightedBalances1;
		private final WeightedBalances weightedBalances2;

		public TestContext() {
			BlockHeight height = new BlockHeight(111);
			this.account1 = Mockito.mock(Account.class);
			this.importance1 = Mockito.mock(AccountImportance.class);
			this.weightedBalances1 = Mockito.mock(WeightedBalances.class);
			this.hook(this.account1, this.importance1, this.weightedBalances1, height);

			this.account2 = Mockito.mock(Account.class);
			this.importance2 = Mockito.mock(AccountImportance.class);
			this.weightedBalances2 = Mockito.mock(WeightedBalances.class);
			this.hook(this.account2, this.importance2, this.weightedBalances2, height);
		}

		private void hook(final Account account, final AccountImportance importance, final WeightedBalances weightedBalances, final BlockHeight height) {
			Mockito.when(account.getAddress()).thenReturn(Utils.generateRandomAddress());
			Mockito.when(account.getImportanceInfo()).thenReturn(importance);
			Mockito.when(account.getWeightedBalances()).thenReturn(weightedBalances);
			Mockito.when(weightedBalances.getUnvested(height)).thenReturn(Amount.fromNem(7));
			Mockito.when(weightedBalances.getVested(height)).thenReturn(Amount.fromNem(3));
		}
	}
}