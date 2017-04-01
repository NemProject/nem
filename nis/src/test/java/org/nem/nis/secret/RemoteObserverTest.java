package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.test.*;

public class RemoteObserverTest {

	//region execute

	@Test
	public void notifyExecuteAddsCorrectLessorRemoteLinkInModeActivate() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new ImportanceTransferNotification(context.lessor, context.lessee, ImportanceTransferMode.Activate),
				NisUtils.createBlockNotificationContext(new BlockHeight(7), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.lessorRemoteLinks, Mockito.only())
				.addLink(RemoteLinkFactory.activateHarvestingRemotely(context.lessee.getAddress(), new BlockHeight(7)));
	}

	@Test
	public void notifyExecuteAddsCorrectLessorRemoteLinkInModeDeactivate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.linkLessorToLessee();

		// Act: deliberately supply a fake lessee account
		context.observer.notify(
				new ImportanceTransferNotification(context.lessor, context.fakeLessee, ImportanceTransferMode.Deactivate),
				NisUtils.createBlockNotificationContext(new BlockHeight(7), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.lessorRemoteLinks, Mockito.times(1))
				.addLink(RemoteLinkFactory.deactivateHarvestingRemotely(context.lessee.getAddress(), new BlockHeight(7)));
	}

	@Test
	public void notifyExecuteAddsCorrectLesseeRemoteLinkInModeActivate() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new ImportanceTransferNotification(context.lessor, context.lessee, ImportanceTransferMode.Activate),
				NisUtils.createBlockNotificationContext(new BlockHeight(7), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.lesseeRemoteLinks, Mockito.only())
				.addLink(RemoteLinkFactory.activateRemoteHarvester(context.lessor.getAddress(), new BlockHeight(7)));
	}

	//endregion

	//region undo

	@Test
	public void notifyUndoRemovesCorrectLessorRemoteLinkInModeActivate() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new ImportanceTransferNotification(context.lessor, context.lessee, ImportanceTransferMode.Activate),
				NisUtils.createBlockNotificationContext(new BlockHeight(7), NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(context.lessorRemoteLinks, Mockito.only())
				.removeLink(RemoteLinkFactory.activateHarvestingRemotely(context.lessee.getAddress(), new BlockHeight(7)));
	}

	@Test
	public void notifyUndoRemovesCorrectLessorRemoteLinkInModeDeactivate() {
		// Arrange:
		final TestContext context = new TestContext();
		context.linkLessorToLessee();

		// Act: deliberately supply a fake lessee account
		context.observer.notify(
				new ImportanceTransferNotification(context.lessor, context.fakeLessee, ImportanceTransferMode.Deactivate),
				NisUtils.createBlockNotificationContext(new BlockHeight(7), NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(context.lessorRemoteLinks, Mockito.times(1))
				.removeLink(RemoteLinkFactory.deactivateHarvestingRemotely(context.lessee.getAddress(), new BlockHeight(7)));
	}

	@Test
	public void notifyUndoRemovesCorrectLesseeRemoteLinkInModeActivate() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new ImportanceTransferNotification(context.lessor, context.lessee, ImportanceTransferMode.Activate),
				NisUtils.createBlockNotificationContext(new BlockHeight(7), NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(context.lesseeRemoteLinks, Mockito.only())
				.removeLink(RemoteLinkFactory.activateRemoteHarvester(context.lessor.getAddress(), new BlockHeight(7)));
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
		private final Account fakeLessee = Utils.generateRandomAccount();
		private final RemoteLinks lessorRemoteLinks = Mockito.mock(RemoteLinks.class);
		private final RemoteLinks lesseeRemoteLinks = Mockito.mock(RemoteLinks.class);
		private final RemoteLinks fakeLesseeRemoteLinks = Mockito.mock(RemoteLinks.class);
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final BlockTransactionObserver observer = new RemoteObserver(this.accountStateCache);

		private TestContext() {
			this.hook(this.lessor, this.lessorRemoteLinks);
			this.hook(this.lessee, this.lesseeRemoteLinks);
			this.hook(this.fakeLessee, this.fakeLesseeRemoteLinks);
		}

		private void hook(final Account account, final RemoteLinks remoteLinks) {
			final AccountState state = Mockito.mock(AccountState.class);
			Mockito.when(state.getRemoteLinks()).thenReturn(remoteLinks);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(state);
		}

		private void linkLessorToLessee() {
			final RemoteLink remoteLink = new RemoteLink(
					this.lessee.getAddress(),
					BlockHeight.ONE,
					ImportanceTransferMode.Activate,
					RemoteLink.Owner.HarvestingRemotely);
			Mockito.when(this.lessorRemoteLinks.getCurrent()).thenReturn(remoteLink);
		}
	}
}
