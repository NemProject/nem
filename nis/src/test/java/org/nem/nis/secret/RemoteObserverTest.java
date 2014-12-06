package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.poi.*;
import org.nem.nis.test.NisUtils;

public class RemoteObserverTest {

	//region execute

	@Test
	public void notifyExecuteAddsCorrectLessorRemoteLink() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new ImportanceTransferNotification(context.lessor, context.lessee, 11),
				NisUtils.createBlockNotificationContext(new BlockHeight(7), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.lessorRemoteLinks, Mockito.only())
				.addLink(new RemoteLink(context.lessee.getAddress(), new BlockHeight(7), 11, RemoteLink.Owner.HarvestingRemotely));
	}

	@Test
	public void notifyExecuteAddsCorrectLesseeRemoteLink() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new ImportanceTransferNotification(context.lessor, context.lessee, 11),
				NisUtils.createBlockNotificationContext(new BlockHeight(7), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.lesseeRemoteLinks, Mockito.only())
				.addLink(new RemoteLink(context.lessor.getAddress(), new BlockHeight(7), 11, RemoteLink.Owner.RemoteHarvester));
	}

	//endregion

	//region undo

	@Test
	public void notifyUndoRemovesCorrectLessorRemoteLink() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new ImportanceTransferNotification(context.lessor, context.lessee, 11),
				NisUtils.createBlockNotificationContext(new BlockHeight(7), NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(context.lessorRemoteLinks, Mockito.only())
				.removeLink(new RemoteLink(context.lessee.getAddress(), new BlockHeight(7), 11, RemoteLink.Owner.HarvestingRemotely));
	}

	@Test
	public void notifyUndoRemovesCorrectLesseeRemoteLink() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new ImportanceTransferNotification(context.lessor, context.lessee, 11),
				NisUtils.createBlockNotificationContext(new BlockHeight(7), NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(context.lesseeRemoteLinks, Mockito.only())
				.removeLink(new RemoteLink(context.lessor.getAddress(), new BlockHeight(7), 11, RemoteLink.Owner.RemoteHarvester));
	}

	//endregion

	//region other type

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, context.lessor, Amount.fromNem(22)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.lessorRemoteLinks, Mockito.never()).addLink(Mockito.any());
		Mockito.verify(context.lessorRemoteLinks, Mockito.never()).removeLink(Mockito.any());
	}

	//endregion

	private static class TestContext {
		private final Account lessor = Utils.generateRandomAccount();
		private final Account lessee = Utils.generateRandomAccount();
		private final RemoteLinks lessorRemoteLinks = Mockito.mock(RemoteLinks.class);
		private final RemoteLinks lesseeRemoteLinks = Mockito.mock(RemoteLinks.class);
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final BlockTransactionObserver observer = new RemoteObserver(this.poiFacade);

		private TestContext() {
			this.hook(this.lessor, this.lessorRemoteLinks);
			this.hook(this.lessee, this.lesseeRemoteLinks);
		}

		private void hook(final Account account, final RemoteLinks remoteLinks) {
			final PoiAccountState state = Mockito.mock(PoiAccountState.class);
			Mockito.when(state.getRemoteLinks()).thenReturn(remoteLinks);
			Mockito.when(this.poiFacade.findStateByAddress(account.getAddress())).thenReturn(state);
		}
	}
}
