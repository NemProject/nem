package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
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

import java.util.*;

import static org.nem.core.test.Utils.createMosaicProperties;

public class ExpiredNamespacesObserverTest {
	private static final int ESTIMATED_BLOCKS_PER_YEAR = 1234;
	private static final int NOTIFY_BLOCK_HEIGHT = 123 + ESTIMATED_BLOCKS_PER_YEAR;

	// region execute

	@Test
	public void notifyExecuteIsNoopIfNoRootNamespaceExpiredAtContextHeight() {
		// Assert:
		assertNoAction(new BlockHeight(234), NotificationTrigger.Execute);
	}

	@Test
	public void notifyExecuteRemovesMosaicIdsFromAccountIfAssociatedNamespaceExpiredAtContextHeight() {
		// Arrange:
		final TestContext context = new TestContext(new BlockHeight(321), new BlockHeight(123));
		context.addMosaicsToAccounts();

		// Sanity:
		assertAccountOwnsMosaics(context.accountInfo1, Collections.singletonList(context.mosaicDefinition1.getId()));
		assertAccountOwnsMosaics(context.accountInfo2, Arrays.asList(context.mosaicDefinition1.getId(), context.mosaicDefinition2.getId()));

		// Act:
		// namespace 2 will expire, namespace 1 not.
		notify(context, NotificationTrigger.Execute);

		// Assert:
		// mosaic id 1 should still be with both accounts
		// mosaic id 2 should have been removed from account 2
		Mockito.verify(context.accountStateCache, Mockito.never()).findStateByAddress(context.mosaicDefinition1.getCreator().getAddress());
		Mockito.verify(context.accountStateCache, Mockito.only()).findStateByAddress(context.mosaicDefinition2.getCreator().getAddress());
		assertAccountOwnsMosaics(context.accountInfo1, Collections.singletonList(context.mosaicDefinition1.getId()));
		assertAccountOwnsMosaics(context.accountInfo2, Collections.singletonList(context.mosaicDefinition1.getId()));
	}

	// endregion

	// region undo

	@Test
	public void notifyUndoIsNoOpIfNoRootNamespaceExpiredAtContextHeight() {
		// Assert:
		assertNoAction(new BlockHeight(234), NotificationTrigger.Undo);
	}

	@Test
	public void notifyUndoAddsMosaicIdsToAccountIfAssociatedNamespaceExpiredAtContextHeight() {
		// Arrange:
		final TestContext context = new TestContext(new BlockHeight(321), new BlockHeight(123));

		// Sanity:
		assertAccountOwnsMosaics(context.accountInfo1, Collections.emptyList());
		assertAccountOwnsMosaics(context.accountInfo2, Collections.emptyList());

		// Act: namespace 2 will be brought back to life, namespace 1 not.
		notify(context, NotificationTrigger.Undo);

		// Assert: mosaic id 2 should have been added to account 2, account 1 should be unchanged
		Mockito.verify(context.accountStateCache, Mockito.never()).findStateByAddress(context.mosaicDefinition1.getCreator().getAddress());
		Mockito.verify(context.accountStateCache, Mockito.only()).findStateByAddress(context.mosaicDefinition2.getCreator().getAddress());
		assertAccountOwnsMosaics(context.accountInfo1, Collections.emptyList());
		assertAccountOwnsMosaics(context.accountInfo2, Collections.singletonList(context.mosaicDefinition2.getId()));
	}

	// endregion

	private static void assertAccountOwnsMosaics(final AccountInfo accountInfo, final Collection<MosaicId> mosaicIds) {
		MatcherAssert.assertThat(accountInfo.getMosaicIds(), IsEquivalent.equivalentTo(mosaicIds));
	}

	private static void assertNoAction(final BlockHeight height, final NotificationTrigger notificationTrigger) {
		// Arrange:
		final TestContext context = new TestContext(height, height);
		context.addMosaicsToAccounts();

		// Sanity:
		assertAccountOwnsMosaics(context.accountInfo1, Collections.singletonList(context.mosaicDefinition1.getId()));
		assertAccountOwnsMosaics(context.accountInfo2, Arrays.asList(context.mosaicDefinition1.getId(), context.mosaicDefinition2.getId()));

		// Act:
		notify(context, notificationTrigger);

		// Assert: The account state cache should not have been touched
		Mockito.verify(context.accountStateCache, Mockito.never()).findStateByAddress(Mockito.any());
	}

	private static void notify(final TestContext context, final NotificationTrigger notificationTrigger) {
		// Arrange:
		final ExpiredNamespacesObserver observer = context.createObserver();

		// Act:
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BlockHarvest, Utils.generateRandomAccount(), Amount.ZERO),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private static class TestContext {
		private final MosaicDefinition mosaicDefinition1 = Utils.createMosaicDefinition(1, createMosaicProperties());
		private final MosaicDefinition mosaicDefinition2 = Utils.createMosaicDefinition(2, createMosaicProperties());
		private final DefaultNamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		private final DefaultAccountStateCache accountStateCache = Mockito.mock(DefaultAccountStateCache.class);
		private final AccountInfo accountInfo1;
		private final AccountInfo accountInfo2;

		private TestContext(final BlockHeight height1, final BlockHeight height2) {
			// mosaic 1
			final NamespaceId namespaceId1 = this.mosaicDefinition1.getId().getNamespaceId();
			this.namespaceCache.add(new Namespace(namespaceId1, this.mosaicDefinition1.getCreator(), height1));
			final Mosaics mosaics1 = this.namespaceCache.get(namespaceId1).getMosaics();
			mosaics1.add(this.mosaicDefinition1);

			// mosaic 2
			final NamespaceId namespaceId2 = this.mosaicDefinition2.getId().getNamespaceId();
			this.namespaceCache.add(new Namespace(namespaceId2, this.mosaicDefinition2.getCreator(), height2));
			final Mosaics mosaics2 = this.namespaceCache.get(namespaceId2).getMosaics();
			mosaics2.add(this.mosaicDefinition2);

			// account info 1
			final Address address1 = mosaicDefinition1.getCreator().getAddress();
			final AccountState accountState1 = new AccountState(address1);
			this.accountInfo1 = accountState1.getAccountInfo();
			Mockito.when(this.accountStateCache.findStateByAddress(address1)).thenReturn(accountState1);

			// account info 2
			final Address address2 = mosaicDefinition2.getCreator().getAddress();
			final AccountState accountState2 = new AccountState(address2);
			this.accountInfo2 = accountState2.getAccountInfo();
			Mockito.when(this.accountStateCache.findStateByAddress(address2)).thenReturn(accountState2);

			this.addMosaicsToNamespaceMosaicsBalances();
		}

		private ExpiredNamespacesObserver createObserver() {
			return new ExpiredNamespacesObserver(this.namespaceCache, this.accountStateCache, ESTIMATED_BLOCKS_PER_YEAR);
		}

		private void addMosaicsToNamespaceMosaicsBalances() {
			final MosaicId id1 = this.mosaicDefinition1.getId();
			this.namespaceCache.get(id1.getNamespaceId()).getMosaics().get(id1).getBalances()
					.incrementBalance(mosaicDefinition1.getCreator().getAddress(), Quantity.fromValue(1));

			final MosaicId id2 = this.mosaicDefinition2.getId();
			this.namespaceCache.get(id2.getNamespaceId()).getMosaics().get(id2).getBalances()
					.incrementBalance(mosaicDefinition2.getCreator().getAddress(), Quantity.fromValue(1));
		}

		// account 1: owns only mosaic 1
		// account 2: owns mosaic 1 and mosaic 2
		private void addMosaicsToAccounts() {
			this.accountInfo1.addMosaicId(mosaicDefinition1.getId());
			this.accountInfo2.addMosaicId(mosaicDefinition1.getId());
			this.accountInfo2.addMosaicId(mosaicDefinition2.getId());
		}
	}
}
