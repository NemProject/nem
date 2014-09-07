package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.ImportanceTransferTransactionMode;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.poi.RemoteState;

public class RemoteObserverTest {

	@Test
	public void notifyTransferForwardsToPoiAccountState() {
		// Arrange:
		final TestDummyContext context = new TestDummyContext();
		final RemoteObserver observer = context.createObserver(new BlockHeight(123), true);

		// Act:
		observer.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Activate);

		// Assert:
		context.verifyForward();
	}

	@Test
	public void notifyTransferSetsProperFields() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver observer = context.createObserver(new BlockHeight(123), true);

		// Act:
		observer.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Activate);

		// Assert:
		context.verifyForward();
	}

	@Test
	public void canRollbackTransfer() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver forward = context.createObserver(new BlockHeight(123), true);
		final RemoteObserver rollback = context.createObserver(new BlockHeight(123), false);

		// Act:
		forward.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Activate);
		rollback.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Activate);

		// Assert:
		context.verifyEmpty();
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidHeightInRollbackThrowsException() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver forward = context.createObserver(new BlockHeight(123), true);
		final RemoteObserver rollback = context.createObserver(new BlockHeight(124), false);

		// Act:
		forward.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Activate);
		rollback.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Activate);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidDirection1InRollbackThrowsException() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver forward = context.createObserver(new BlockHeight(123), true);
		final RemoteObserver rollback = context.createObserver(new BlockHeight(123), false);

		// Act:
		forward.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Activate);
		rollback.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Deactivate);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidDirection2InRollbackThrowsException() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver forward = context.createObserver(new BlockHeight(123), true);
		final RemoteObserver rollback = context.createObserver(new BlockHeight(123), false);

		// Act:
		forward.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Deactivate);
		rollback.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Activate);
	}

	@Test
	public void rollbackRestoresPreviousState() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver forward = context.createObserver(new BlockHeight(123), true);
		final RemoteObserver cancel = context.createObserver(new BlockHeight(123 + 1440), true);
		final RemoteObserver rollback = context.createObserver(new BlockHeight(123 + 1440), false);

		// Act:
		forward.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Activate);
		cancel.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Deactivate);
		rollback.notifyTransfer(context.account1, context.account2, ImportanceTransferTransactionMode.Deactivate);

		// Assert:
		context.verifyForward();
	}

	private class TestDummyContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final PoiAccountState poiAccount1State = Mockito.mock(PoiAccountState.class);
		private final PoiAccountState poiAccount2State = Mockito.mock(PoiAccountState.class);
		public final Account account1 = Utils.generateRandomAccount();
		public final Account account2 = Utils.generateRandomAccount();

		public TestDummyContext() {
			this.hook();
		}

		public RemoteObserver createObserver(final BlockHeight height, boolean isExecute) {
			return new RemoteObserver(this.poiFacade, height, isExecute);
		}

		public void verifyForward() {
			Mockito.verify(poiAccount1State, Mockito.times(1)).setRemote(Mockito.any(), Mockito.any(), Mockito.anyInt());
			Mockito.verify(poiAccount2State, Mockito.times(1)).remoteFor(Mockito.any(), Mockito.any(), Mockito.anyInt());
		}

		private void hook() {
			Mockito.when(this.poiFacade.findStateByAddress(account1.getAddress())).thenReturn(poiAccount1State);
			Mockito.when(this.poiFacade.findStateByAddress(account2.getAddress())).thenReturn(poiAccount2State);
			Mockito.when(poiAccount1State.getAddress()).thenReturn(account1.getAddress());
			Mockito.when(poiAccount2State.getAddress()).thenReturn(account2.getAddress());
		}
	}

	private class TestContext {
		private final PoiFacade poiFacade = new PoiFacade(null);
		public final Account account1 = Utils.generateRandomAccount();
		public final Account account2 = Utils.generateRandomAccount();

		public RemoteObserver createObserver(final BlockHeight height, boolean isExecute) {
			return new RemoteObserver(this.poiFacade, height, isExecute);
		}

		public void verifyForward() {
			final RemoteState remoteState1 = stateForAccount(this.account1).getRemoteState();
			final RemoteState remoteState2 = stateForAccount(this.account2).getRemoteState();
			Assert.assertTrue(remoteState1.hasRemote());
			Assert.assertTrue(remoteState2.hasRemote());
			Assert.assertTrue(remoteState1.isOwner());
			Assert.assertFalse(remoteState2.isOwner());
			Assert.assertThat(remoteState1.getRemoteAddress(), IsEqual.equalTo(this.account2.getAddress()));
			Assert.assertThat(remoteState2.getRemoteAddress(), IsEqual.equalTo(this.account1.getAddress()));
		}

		private PoiAccountState stateForAccount(Account account) {
			 return this.poiFacade.findStateByAddress(account.getAddress());
		}

		public void verifyEmpty() {
			final PoiAccountState poiAccountState1 = stateForAccount(this.account1);
			final PoiAccountState poiAccountState2 = stateForAccount(this.account2);
			Assert.assertFalse(poiAccountState1.hasRemoteState());
			Assert.assertFalse(poiAccountState2.hasRemoteState());
		}
	}
}
