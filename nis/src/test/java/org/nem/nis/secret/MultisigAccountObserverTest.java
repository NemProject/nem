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

import java.util.*;

public class MultisigAccountObserverTest {

	//region AddCosignatory

	@Test
	public void notifyTransferExecuteAddAddsMultisigLinks() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyCosignatoryModification(context, MultisigModificationType.AddCosignatory, NotificationTrigger.Execute);

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.times(1)).addCosignatory(context.account2.getAddress());
		Mockito.verify(context.multisigLinks2, Mockito.times(1)).addCosignatoryOf(context.account1.getAddress());
	}

	@Test
	public void notifyTransferUndoAddRemovesMultisigLinks() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyCosignatoryModification(context, MultisigModificationType.AddCosignatory, NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.times(1)).removeCosignatory(context.account2.getAddress());
		Mockito.verify(context.multisigLinks2, Mockito.times(1)).removeCosignatoryOf(context.account1.getAddress());
	}

	//endregion

	//region DelCosignatory

	@Test
	public void notifyTransferExecuteDelRemovedMultisigLinks() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyCosignatoryModification(context, MultisigModificationType.DelCosignatory, NotificationTrigger.Execute);

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.times(1)).removeCosignatory(context.account2.getAddress());
		Mockito.verify(context.multisigLinks2, Mockito.times(1)).removeCosignatoryOf(context.account1.getAddress());
	}

	@Test
	public void notifyTransferUndoDelAddsMultisigLinks() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyCosignatoryModification(context, MultisigModificationType.DelCosignatory, NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.times(1)).addCosignatory(context.account2.getAddress());
		Mockito.verify(context.multisigLinks2, Mockito.times(1)).addCosignatoryOf(context.account1.getAddress());
	}

	//endregion

	//region MinCosignatories

	@Test
	public void notifyTransferExecuteCallsIncrementMinCosignatoriesByWithModificationValue() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyMinCosignatoriesModification(context, NotificationTrigger.Execute);

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.times(1)).incrementMinCosignatoriesBy(12);
	}

	@Test
	public void notifyTransferUndoCallsIncrementMinCosignatoriesByWithNegativeModificationValue() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyMinCosignatoriesModification(context, NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.times(1)).incrementMinCosignatoriesBy(-12);
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

	private void notifyCosignatoryModification(
			final TestContext context,
			final MultisigModificationType value,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final MultisigAccountObserver observer = context.createObserver();

		// Act:
		final MultisigCosignatoryModification cosignatoryModification = new MultisigCosignatoryModification(value, context.account2);
		observer.notify(
				new MultisigCosignatoryModificationNotification(context.account1, cosignatoryModification),
				NisUtils.createBlockNotificationContext(new BlockHeight(111), notificationTrigger));
	}

	private void notifyMinCosignatoriesModification(
			final TestContext context,
			final NotificationTrigger notificationTrigger) {

		// Arrange:
		final MultisigAccountObserver observer = context.createObserver();

		// Act:
		final MultisigMinCosignatoriesModification minCosignatoriesModification = new MultisigMinCosignatoriesModification(12);
		observer.notify(
				new MultisigMinCosignatoriesModificationNotification(context.account1, minCosignatoriesModification),
				NisUtils.createBlockNotificationContext(new BlockHeight(111), notificationTrigger));
	}

	private class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final Account account1 = Utils.generateRandomAccount();
		private final Account account2 = Utils.generateRandomAccount();
		private final List<Address> cosignatories = new ArrayList<>();
		final MultisigLinks multisigLinks1 = Mockito.mock(MultisigLinks.class);
		final MultisigLinks multisigLinks2 = Mockito.mock(MultisigLinks.class);
		final AccountState account1State = Mockito.mock(AccountState.class);
		final AccountState account2State = Mockito.mock(AccountState.class);

		private TestContext() {
			Mockito.when(this.multisigLinks1.getCosignatories()).thenReturn(this.cosignatories);
		}

		private MultisigAccountObserver createObserver() {
			this.hook(this.account1, this.account1State, this.multisigLinks1);
			this.hook(this.account2, this.account2State, this.multisigLinks2);
			return new MultisigAccountObserver(this.accountStateCache);
		}

		private void hook(final Account account, final AccountState accountState, final MultisigLinks multisigLinks) {
			final Address address = account.getAddress();
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			Mockito.when(accountState.getMultisigLinks()).thenReturn(multisigLinks);
			Mockito.when(accountState.getAddress()).thenReturn(address);
		}
	}
}
