package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

import java.util.Collections;

import static org.nem.core.test.Utils.createMosaicProperties;

public class ExpiredNamespacesObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 123 + 365 * 1440;

	// region execute

	@Test
	public void notifyExecuteCallsIsNoopIfNoRootNamespaceExpiredAtContextHeight() {
		// Assert:
		assertNoAction(new BlockHeight(234), NotificationTrigger.Execute);
	}

	@Test
	public void notifyExecuteRemovesMosaicIdsFromAccountIfAssociatedNamespaceExpiredAtContextHeight() {
		// Arrange:
		final TestContext context = new TestContext(new BlockHeight(123));
		context.addMosaicToAccount();

		// Sanity:
		Assert.assertThat(
				context.accountInfo.getMosaicIds(),
				IsEquivalent.equivalentTo(Collections.singletonList(context.mosaicDefinition.getId())));

		// Act:
		notify(context, NotificationTrigger.Execute);

		// Assert: the mosaic id should have been removed
		Mockito.verify(context.accountStateCache, Mockito.only()).findStateByAddress(context.mosaicDefinition.getCreator().getAddress());
		Assert.assertThat(context.accountInfo.getMosaicIds().isEmpty(), IsEqual.equalTo(true));
	}

	// endregion

	// region undo

	@Test
	public void notifyUndoCallsIsNoopIfNoRootNamespaceExpiredAtContextHeight() {
		// Assert:
		assertNoAction(new BlockHeight(234), NotificationTrigger.Undo);
	}

	@Test
	public void notifyUndoAddsMosaicIdsToAccountIfAssociatedNamespaceExpiredAtContextHeight() {
		// Arrange:
		final TestContext context = new TestContext(new BlockHeight(123));

		// Sanity:
		Assert.assertThat(context.accountInfo.getMosaicIds().isEmpty(), IsEqual.equalTo(true));

		// Act:
		notify(context, NotificationTrigger.Undo);

		// Assert: the mosaic id should have been added
		Mockito.verify(context.accountStateCache, Mockito.only()).findStateByAddress(context.mosaicDefinition.getCreator().getAddress());
		Assert.assertThat(
				context.accountInfo.getMosaicIds(),
				IsEquivalent.equivalentTo(Collections.singletonList(context.mosaicDefinition.getId())));
	}

	// endregion

	private static void assertNoAction(final BlockHeight height, final NotificationTrigger notificationTrigger) {
		// Arrange:
		final TestContext context = new TestContext(height);
		context.addMosaicToAccount();

		// Sanity:
		Assert.assertThat(
				context.accountInfo.getMosaicIds(),
				IsEquivalent.equivalentTo(Collections.singletonList(context.mosaicDefinition.getId())));

		// Act:
		notify(context, notificationTrigger);

		// Assert: The account state cache should not have been touched
		Mockito.verify(context.accountStateCache, Mockito.never()).findStateByAddress(Mockito.any());
	}

	private static void notify(
			final TestContext context,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final ExpiredNamespacesObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BlockHarvest, Utils.generateRandomAccount(), Amount.ZERO),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private static class TestContext {
		private final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(1, createMosaicProperties());
		private final DefaultNamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		private final DefaultAccountStateCache accountStateCache = Mockito.mock(DefaultAccountStateCache.class);
		private final AccountInfo accountInfo;

		private TestContext(final BlockHeight height) {
			final NamespaceId namespaceId = this.mosaicDefinition.getId().getNamespaceId();
			this.namespaceCache.add(new Namespace(namespaceId, this.mosaicDefinition.getCreator(), height));
			final Mosaics mosaics = this.namespaceCache.get(namespaceId).getMosaics();
			mosaics.add(this.mosaicDefinition);

			final Address address = mosaicDefinition.getCreator().getAddress();
			final AccountState accountState = new AccountState(address);
			accountInfo = accountState.getAccountInfo();
			Mockito.when(this.accountStateCache.findStateByAddress(address)).thenReturn(accountState);

			this.addMosaicToNamespaceMosaicsBalances();
		}

		private ExpiredNamespacesObserver createObserver() {
			return new ExpiredNamespacesObserver(this.namespaceCache, this.accountStateCache);
		}

		private void addMosaicToNamespaceMosaicsBalances() {
			final MosaicId id = this.mosaicDefinition.getId();
			this.namespaceCache.get(id.getNamespaceId()).getMosaics().get(id).getBalances().incrementBalance(
					mosaicDefinition.getCreator().getAddress(),
					Quantity.fromValue(1));
		}

		private void addMosaicToAccount() {
			this.accountInfo.addMosaicId(mosaicDefinition.getId());
		}
	}
}
