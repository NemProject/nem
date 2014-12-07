package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.poi.*;
import org.nem.nis.test.NisUtils;

import java.util.Arrays;
import java.util.List;

public class MultisigAccountObserverTest {
	@Test
	public void notifyTransferExecuteAddAddsMultisigLinks() {
		final TestContext context = notifyTransferPrepare(MultisigModificationType.Add, NotificationTrigger.Execute);

		// Assert:
		Mockito.verify(context.poiAccount1State, Mockito.times(1)).getMultisigLinks().addCosignatory(context.account2.getAddress(), new BlockHeight(111));
		Mockito.verify(context.poiAccount2State, Mockito.times(1)).getMultisigLinks().addMultisig(context.account1.getAddress(), new BlockHeight(111));
	}

	@Test
	public void notifyTransferUndoAddRemovesMultisigLinks() {
		final TestContext context = notifyTransferPrepare(MultisigModificationType.Add, NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.poiAccount1State, Mockito.times(1)).getMultisigLinks().removeCosignatory(context.account2.getAddress(), new BlockHeight(111));
		Mockito.verify(context.poiAccount2State, Mockito.times(1)).getMultisigLinks().removeMultisig(context.account1.getAddress(), new BlockHeight(111));
	}

	// TODO: This test is wrong, it should use MultisigModificationType.Del
	@Test
	public void notifyTransferExecuteDelRemovedMultisigLinks() {
		final TestContext context = notifyTransferPrepare(MultisigModificationType.Unknown, NotificationTrigger.Execute);

		// Assert:
		Mockito.verify(context.poiAccount1State, Mockito.times(1)).getMultisigLinks().removeCosignatory(context.account2.getAddress(), new BlockHeight(111));
		Mockito.verify(context.poiAccount2State, Mockito.times(1)).getMultisigLinks().removeMultisig(context.account1.getAddress(), new BlockHeight(111));
	}

	// TODO: This test is wrong, it should use MultisigModificationType.Del
	@Test
	public void notifyTransferUndoDelAddsMultisigLinks() {
		final TestContext context = notifyTransferPrepare(MultisigModificationType.Unknown, NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.poiAccount1State, Mockito.times(1)).getMultisigLinks().addCosignatory(context.account2.getAddress(), new BlockHeight(111));
		Mockito.verify(context.poiAccount2State, Mockito.times(1)).getMultisigLinks().addMultisig(context.account1.getAddress(), new BlockHeight(111));
	}


	private TestContext notifyTransferPrepare(final MultisigModificationType value, final NotificationTrigger notificationTrigger) {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigAccountObserver observer = context.createObserver();

		// Act:
		final List<MultisigModification> modifications = Arrays.asList(new MultisigModification(value, context.account2));
		observer.notify(
				new MultisigModificationNotification(context.account1, modifications),
				NisUtils.createBlockNotificationContext(new BlockHeight(111), notificationTrigger));
		return context;
	}

	private class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final Account account1 = Utils.generateRandomAccount();
		private final Account account2 = Utils.generateRandomAccount();
		final PoiAccountState poiAccount1State = Mockito.mock(PoiAccountState.class);
		final PoiAccountState poiAccount2State = Mockito.mock(PoiAccountState.class);

		public MultisigAccountObserver createObserver() {
			this.hook(this.account1, this.poiAccount1State);
			this.hook(this.account2, this.poiAccount2State);
			return new MultisigAccountObserver(this.poiFacade);
		}

		public void hook(final Account account, final PoiAccountState accountState) {
			final Address address = account.getAddress();
			Mockito.when(this.poiFacade.findStateByAddress(account.getAddress())).thenReturn(accountState);
			Mockito.when(accountState.getAddress()).thenReturn(address);
		}
	}
}
