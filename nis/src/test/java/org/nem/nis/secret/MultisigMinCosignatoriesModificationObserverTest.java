package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

public class MultisigMinCosignatoriesModificationObserverTest {

	// region MinCosignatories

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

	// endregion

	// region other type

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigMinCosignatoriesModificationObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new MultisigCosignatoryModificationNotification(context.account1,
						new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount())),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.multisigLinks1, Mockito.never()).incrementMinCosignatoriesBy(Mockito.anyInt());
	}

	// endregion

	private void notifyMinCosignatoriesModification(final TestContext context, final NotificationTrigger notificationTrigger) {
		// Arrange:
		final MultisigMinCosignatoriesModificationObserver observer = context.createObserver();

		// Act:
		final MultisigMinCosignatoriesModification minCosignatoriesModification = new MultisigMinCosignatoriesModification(12);
		observer.notify(new MultisigMinCosignatoriesModificationNotification(context.account1, minCosignatoriesModification),
				NisUtils.createBlockNotificationContext(new BlockHeight(111), notificationTrigger));
	}

	private class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final Account account1 = Utils.generateRandomAccount();
		final MultisigLinks multisigLinks1 = Mockito.mock(MultisigLinks.class);
		final AccountState account1State = Mockito.mock(AccountState.class);

		private MultisigMinCosignatoriesModificationObserver createObserver() {
			this.hook(this.account1, this.account1State, this.multisigLinks1);
			return new MultisigMinCosignatoriesModificationObserver(this.accountStateCache);
		}

		private void hook(final Account account, final AccountState accountState, final MultisigLinks multisigLinks) {
			final Address address = account.getAddress();
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			Mockito.when(accountState.getMultisigLinks()).thenReturn(multisigLinks);
			Mockito.when(accountState.getAddress()).thenReturn(address);
		}
	}
}
