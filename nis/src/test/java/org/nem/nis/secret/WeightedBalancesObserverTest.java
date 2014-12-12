package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.PoiFacade;
import org.nem.nis.state.*;

public class WeightedBalancesObserverTest {

	@Test
	public void notifySendCallsWeightedBalancesAddSend() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notifySend(new BlockHeight(123), context.account, new Amount(54));

		// Assert:
		Mockito.verify(context.balances, Mockito.times(1)).addSend(new BlockHeight(123), new Amount(54));
		verifyCallCounts(context.balances, 1, 0, 0, 0);
	}

	@Test
	public void notifyReceiveCallsWeightedBalancesAddReceive() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notifyReceive(new BlockHeight(123), context.account, new Amount(54));

		// Assert:
		Mockito.verify(context.balances, Mockito.times(1)).addReceive(new BlockHeight(123), new Amount(54));
		verifyCallCounts(context.balances, 0, 1, 0, 0);
	}

	@Test
	public void notifySendUndoCallsWeightedBalancesUndoSend() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notifySendUndo(new BlockHeight(123), context.account, new Amount(54));

		// Assert:
		Mockito.verify(context.balances, Mockito.times(1)).undoSend(new BlockHeight(123), new Amount(54));
		verifyCallCounts(context.balances, 0, 0, 1, 0);
	}

	@Test
	public void notifyReceiveUndoCallsWeightedBalancesUndoReceive() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notifyReceiveUndo(new BlockHeight(123), context.account, new Amount(54));

		// Assert:
		Mockito.verify(context.balances, Mockito.times(1)).undoReceive(new BlockHeight(123), new Amount(54));
		verifyCallCounts(context.balances, 0, 0, 0, 1);
	}

	private static void verifyCallCounts(
			final WeightedBalances balances,
			final int addSendCounts,
			final int addReceiveCounts,
			final int undoSendCounts,
			final int undoReceiveCounts) {
		Mockito.verify(balances, Mockito.times(addSendCounts)).addSend(Mockito.any(), Mockito.any());
		Mockito.verify(balances, Mockito.times(addReceiveCounts)).addReceive(Mockito.any(), Mockito.any());
		Mockito.verify(balances, Mockito.times(undoSendCounts)).undoSend(Mockito.any(), Mockito.any());
		Mockito.verify(balances, Mockito.times(undoReceiveCounts)).undoReceive(Mockito.any(), Mockito.any());
	}

	private static class TestContext {
		private final Account account;
		private final WeightedBalances balances;
		private final WeightedBalancesObserver observer;

		public TestContext() {
			final Address address = Utils.generateRandomAddress();
			this.account = Mockito.mock(Account.class);
			Mockito.when(this.account.getAddress()).thenReturn(address);

			this.balances = Mockito.mock(WeightedBalances.class);

			final PoiAccountState accountState = Mockito.mock(PoiAccountState.class);
			Mockito.when(accountState.getWeightedBalances()).thenReturn(this.balances);

			final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
			Mockito.when(poiFacade.findStateByAddress(address)).thenReturn(accountState);

			this.observer = new WeightedBalancesObserver(poiFacade);
		}
	}
}