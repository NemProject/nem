package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;

import java.util.function.Consumer;

public class WeightedBalancesObserverTest {

	// region check for zero amount

	@Test
	public void notifySendDoesNothingIfAmountIsZero() {
		// Assert:
		assertBalancesIsEmpty(observer -> observer.notifySend(new BlockHeight(123), Utils.generateRandomAccount(), Amount.ZERO));
	}

	@Test
	public void notifyReceiveDoesNothingIfAmountIsZero() {
		// Assert:
		assertBalancesIsEmpty(observer -> observer.notifyReceive(new BlockHeight(123), Utils.generateRandomAccount(), Amount.ZERO));
	}

	@Test
	public void notifySendUndoDoesNothingIfAmountIsZero() {
		// Assert:
		assertBalancesIsEmpty(observer -> observer.notifySendUndo(new BlockHeight(123), Utils.generateRandomAccount(), Amount.ZERO));
	}

	@Test
	public void notifyReceiveUndoDoesNothingIfAmountIsZero() {
		// Assert:
		assertBalancesIsEmpty(observer -> observer.notifyReceiveUndo(new BlockHeight(123), Utils.generateRandomAccount(), Amount.ZERO));
	}

	private static void assertBalancesIsEmpty(final Consumer<WeightedBalancesObserver> notify) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		notify.accept(context.observer);

		// Assert:
		MatcherAssert.assertThat(context.balances.size(), IsEqual.equalTo(0));
	}

	// endregion

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
		final BlockHeight height = new BlockHeight(123);
		final WeightedBalances balances = new TimeBasedVestingWeightedBalances();
		final TestContext context = new TestContext(Utils.generateRandomAccount(), Mockito.spy(balances));

		// Act:
		context.observer.notifyReceive(height, context.account, new Amount(54));

		// Assert:
		Mockito.verify(context.balances, Mockito.times(1)).addReceive(new BlockHeight(123), new Amount(54));
		Mockito.verify(context.balances, Mockito.never()).convertToFullyVested();
		verifyCallCounts(context.balances, 0, 1, 0, 0);

		MatcherAssert.assertThat(balances.getUnvested(height), IsEqual.equalTo(new Amount(54)));
		MatcherAssert.assertThat(balances.getVested(height), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void notifyReceiveAtHeightOneCallsWeightedBalancesAddReceiveForNemesisAccount() {
		// Arrange:
		final BlockHeight height = new BlockHeight(123);
		final WeightedBalances balances = new TimeBasedVestingWeightedBalances();
		final Address nemesisAddress = NetworkInfos.getDefault().getNemesisBlockInfo().getAddress();
		final TestContext context = new TestContext(new Account(nemesisAddress), Mockito.spy(balances));

		// Act:
		context.observer.notifyReceive(BlockHeight.ONE, context.account, new Amount(54));

		// Assert:
		Mockito.verify(context.balances, Mockito.times(1)).addReceive(BlockHeight.ONE, new Amount(54));
		Mockito.verify(context.balances, Mockito.never()).convertToFullyVested();
		verifyCallCounts(context.balances, 0, 1, 0, 0);

		MatcherAssert.assertThat(balances.getUnvested(height), IsEqual.equalTo(new Amount(54)));
		MatcherAssert.assertThat(balances.getVested(height), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void notifyReceiveAtHeightOneCallsWeightedBalancesAddReceiveAndConvertToFullyVestedForNonNemesisAccount() {
		// Arrange:
		final BlockHeight height = new BlockHeight(123);
		final WeightedBalances balances = new TimeBasedVestingWeightedBalances();
		final TestContext context = new TestContext(Utils.generateRandomAccount(), Mockito.spy(balances));

		// Act:
		context.observer.notifyReceive(BlockHeight.ONE, context.account, new Amount(54));

		// Assert:
		Mockito.verify(context.balances, Mockito.times(1)).addReceive(BlockHeight.ONE, new Amount(54));
		Mockito.verify(context.balances, Mockito.times(1)).convertToFullyVested();
		verifyCallCounts(context.balances, 0, 1, 0, 1);

		MatcherAssert.assertThat(balances.getUnvested(height), IsEqual.equalTo(Amount.ZERO));
		MatcherAssert.assertThat(balances.getVested(height), IsEqual.equalTo(new Amount(54)));
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

	private static void verifyCallCounts(final WeightedBalances balances, final int addSendCounts, final int addReceiveCounts,
			final int undoSendCounts, final int undoReceiveCounts) {
		Mockito.verify(balances, Mockito.times(addSendCounts)).addSend(Mockito.any(), Mockito.any());
		Mockito.verify(balances, Mockito.times(addReceiveCounts)).addReceive(Mockito.any(), Mockito.any());
		Mockito.verify(balances, Mockito.times(undoSendCounts)).undoSend(Mockito.any(), Mockito.any());
		Mockito.verify(balances, Mockito.times(undoReceiveCounts)).undoReceive(Mockito.any(), Mockito.any());
	}

	private static class TestContext {
		private final Account account;
		private final WeightedBalances balances;
		private final WeightedBalancesObserver observer;

		public TestContext(final Account account, final WeightedBalances balances) {
			this.account = account;
			this.balances = balances;

			final AccountState accountState = Mockito.mock(AccountState.class);
			Mockito.when(accountState.getWeightedBalances()).thenReturn(this.balances);

			final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
			Mockito.when(accountStateCache.findStateByAddress(this.account.getAddress())).thenReturn(accountState);

			this.observer = new WeightedBalancesObserver(accountStateCache);
		}

		public TestContext() {
			this(Utils.generateRandomAccount(), Mockito.mock(WeightedBalances.class));
		}
	}
}
