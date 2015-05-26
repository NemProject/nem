package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

public class MultisigAccountObserverTest {

	//region Add

	@Test
	public void notifyTransferExecuteAddAddsMultisigLinks() {
		final TestContext context = this.notifyTransferPrepare(MultisigModificationType.AddCosignatory, NotificationTrigger.Execute);

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.times(1)).addCosignatory(context.account2.getAddress());
		Mockito.verify(context.multisigLinks2, Mockito.times(1)).addCosignatoryOf(context.account1.getAddress());
	}

	@Test
	public void notifyTransferUndoAddRemovesMultisigLinks() {
		final TestContext context = this.notifyTransferPrepare(MultisigModificationType.AddCosignatory, NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.times(1)).removeCosignatory(context.account2.getAddress());
		Mockito.verify(context.multisigLinks2, Mockito.times(1)).removeCosignatoryOf(context.account1.getAddress());
	}

	//endregion

	//region Del

	@Test
	public void notifyTransferExecuteDelRemovedMultisigLinks() {
		final TestContext context = this.notifyTransferPrepare(MultisigModificationType.DelCosignatory, NotificationTrigger.Execute);

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.times(1)).removeCosignatory(context.account2.getAddress());
		Mockito.verify(context.multisigLinks2, Mockito.times(1)).removeCosignatoryOf(context.account1.getAddress());
	}

	@Test
	public void notifyTransferUndoDelAddsMultisigLinks() {
		final TestContext context = this.notifyTransferPrepare(MultisigModificationType.DelCosignatory, NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.times(1)).addCosignatory(context.account2.getAddress());
		Mockito.verify(context.multisigLinks2, Mockito.times(1)).addCosignatoryOf(context.account1.getAddress());
	}

	//endregion

	//region other type

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigAccountObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, context.account1, Amount.fromNem(22)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.never()).addCosignatory(Mockito.any());
		Mockito.verify(context.multisigLinks1, Mockito.never()).addCosignatoryOf(Mockito.any());
		Mockito.verify(context.multisigLinks2, Mockito.never()).addCosignatory(Mockito.any());
		Mockito.verify(context.multisigLinks2, Mockito.never()).addCosignatoryOf(Mockito.any());
	}

	//endregion

	private TestContext notifyTransferPrepare(final MultisigModificationType value, final NotificationTrigger notificationTrigger) {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigAccountObserver observer = context.createObserver();

		// Act:
		final MultisigCosignatoryModification modification = new MultisigCosignatoryModification(value, context.account2);
		observer.notify(
				new MultisigModificationNotification(context.account1, modification),
				NisUtils.createBlockNotificationContext(new BlockHeight(111), notificationTrigger));
		return context;
	}

	private class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final Account account1 = Utils.generateRandomAccount();
		private final Account account2 = Utils.generateRandomAccount();
		final MultisigLinks multisigLinks1 = Mockito.mock(MultisigLinks.class);
		final MultisigLinks multisigLinks2 = Mockito.mock(MultisigLinks.class);
		final AccountState account1State = Mockito.mock(AccountState.class);
		final AccountState account2State = Mockito.mock(AccountState.class);

		public MultisigAccountObserver createObserver() {
			this.hook(this.account1, this.account1State, this.multisigLinks1);
			this.hook(this.account2, this.account2State, this.multisigLinks2);
			return new MultisigAccountObserver(this.accountStateCache);
		}

		public void hook(final Account account, final AccountState accountState, final MultisigLinks multisigLinks) {
			final Address address = account.getAddress();
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			Mockito.when(accountState.getMultisigLinks()).thenReturn(multisigLinks);
			Mockito.when(accountState.getAddress()).thenReturn(address);
		}
	}
}
