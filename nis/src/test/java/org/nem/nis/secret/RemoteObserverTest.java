package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.ImportanceTransferTransactionMode;
import org.nem.core.model.observers.ImportanceTransferNotification;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.poi.RemoteState;

public class RemoteObserverTest {

	private void doNotify(final RemoteObserver observer, final Account account1, final Account account2, final int activate, boolean execute) {
		observer.notify(new ImportanceTransferNotification(account1, account2, activate),
				new BlockNotificationContext(BlockHeight.ONE, execute ? NotificationTrigger.Execute : NotificationTrigger.Undo));
	}

	private void doNotify2(final RemoteObserver observer, final Account account1, final Account account2, final int activate, boolean execute) {
		observer.notify(new ImportanceTransferNotification(account1, account2, activate),
				new BlockNotificationContext(new BlockHeight(2), execute ? NotificationTrigger.Execute : NotificationTrigger.Undo));
	}

	@Test
	public void notifyTransferForwardsToPoiAccountState() {
		// Arrange:
		final TestDummyContext context = new TestDummyContext();
		final RemoteObserver observer = context.createObserver();

		// Act:
		doNotify(observer, context.account1, context.account2, ImportanceTransferTransactionMode.Activate, true);

		// Assert:
		context.verifyForward();
	}

	@Test
	public void notifyTransferSetsProperFields() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver observer = context.createObserver();

		// Act:
		doNotify(observer, context.account1, context.account2, ImportanceTransferTransactionMode.Activate, true);

		// Assert:
		context.verifyForward();
	}

	@Test
	public void canRollbackTransfer() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver forward = context.createObserver();
		final RemoteObserver rollback = context.createObserver();

		// Act:
		doNotify(forward, context.account1, context.account2, ImportanceTransferTransactionMode.Activate, true);
		doNotify(rollback, context.account1, context.account2, ImportanceTransferTransactionMode.Activate, false);

		// Assert:
		context.verifyEmpty();
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidHeightInRollbackThrowsException() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver forward = context.createObserver();
		final RemoteObserver rollback = context.createObserver();

		// Act:
		doNotify(forward, context.account1, context.account2, ImportanceTransferTransactionMode.Activate, true);
		doNotify2(rollback, context.account1, context.account2, ImportanceTransferTransactionMode.Activate, false);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidDirection1InRollbackThrowsException() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver forward = context.createObserver();
		final RemoteObserver rollback = context.createObserver();

		// Act:
		doNotify(forward, context.account1, context.account2, ImportanceTransferTransactionMode.Activate, true);
		doNotify(rollback, context.account1, context.account2, ImportanceTransferTransactionMode.Deactivate, false);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidDirection2InRollbackThrowsException() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver forward = context.createObserver();
		final RemoteObserver rollback = context.createObserver();

		// Act:
		doNotify(forward, context.account1, context.account2, ImportanceTransferTransactionMode.Deactivate, true);
		doNotify(rollback, context.account1, context.account2, ImportanceTransferTransactionMode.Activate, false);
	}

	@Test
	public void rollbackRestoresPreviousState() {
		// Arrange:
		final TestContext context = new TestContext();
		final RemoteObserver forward = context.createObserver();
		final RemoteObserver cancel = context.createObserver();
		final RemoteObserver rollback = context.createObserver();

		// Act:
		doNotify(forward, context.account1, context.account2, ImportanceTransferTransactionMode.Activate, true);
		doNotify(cancel, context.account1, context.account2, ImportanceTransferTransactionMode.Deactivate, true);
		doNotify(rollback, context.account1, context.account2, ImportanceTransferTransactionMode.Deactivate, false);

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

		public RemoteObserver createObserver() {
			return new RemoteObserver(this.poiFacade);
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

		public RemoteObserver createObserver() {
			return new RemoteObserver(this.poiFacade);
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
