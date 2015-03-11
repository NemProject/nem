package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.test.*;

public class RemoteObserverTest {

	//region execute

	@Test
	public void notifyExecuteAddsCorrectLessorRemoteLink() {
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
	public void notifyExecuteAddsCorrectLesseeRemoteLink() {
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
	public void notifyUndoRemovesCorrectLessorRemoteLink() {
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
	public void notifyUndoRemovesCorrectLesseeRemoteLink() {
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
		private final RemoteLinks lessorRemoteLinks = Mockito.mock(RemoteLinks.class);
		private final RemoteLinks lesseeRemoteLinks = Mockito.mock(RemoteLinks.class);
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final BlockTransactionObserver observer = new RemoteObserver(this.accountStateCache);

		private TestContext() {
			this.hook(this.lessor, this.lessorRemoteLinks);
			this.hook(this.lessee, this.lesseeRemoteLinks);
		}

		private void hook(final Account account, final RemoteLinks remoteLinks) {
			final AccountState state = Mockito.mock(AccountState.class);
			Mockito.when(state.getRemoteLinks()).thenReturn(remoteLinks);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(state);
		}
	}
}
