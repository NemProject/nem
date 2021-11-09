package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountInfo;
import org.nem.nis.test.NisUtils;

import java.util.Collections;

public class AccountInfoMosaicIdsObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 111;
	private static final Quantity INITIAL_QUANTITY = MosaicUtils.toQuantity(new Supply(1000), 4);

	// region mosaic creation

	@Test
	public void notifyExecuteMosaicDefinitionCreationUpdatesMosaicIds() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Execute);

		// Assert:
		context.assertMosaicIds(context.sender, true);
		context.assertMosaicIds(context.recipient, false);
	}

	@Test
	public void notifyUndoMosaicDefinitionCreationUpdatesMosaicIds() {
		// Arrange:
		final TestContext context = new TestContext();
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Execute);

		// Act:
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Undo);

		// Assert:
		context.assertMosaicIds(context.sender, false);
		context.assertMosaicIds(context.recipient, false);
	}

	// endregion

	// region mosaic transfer

	@Test
	public void notifyExecuteMosaicTransferWithPartialBalanceTransferUpdatesMosaicIds() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionToCache();

		// Act:
		notifyMosaicTransfer(context, new Quantity(250), NotificationTrigger.Execute);

		// Assert:
		context.assertMosaicIds(context.sender, true);
		context.assertMosaicIds(context.recipient, true);
	}

	@Test
	public void notifyExecuteMosaicTransferWithFullBalanceTransferFromOwnerUpdatesMosaicIds() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionToCache();

		// Act:
		notifyMosaicTransfer(context, INITIAL_QUANTITY, NotificationTrigger.Execute);

		// Assert:
		context.assertMosaicIds(context.sender, true);
		context.assertMosaicIds(context.recipient, true);
		context.assertMosaicIds(context.recipient2, false);
	}

	@Test
	public void notifyExecuteMosaicTransferWithFullBalanceTransferFromNonOwnerUpdatesMosaicIds() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionToCache();
		notifyMosaicTransfer(context, INITIAL_QUANTITY, NotificationTrigger.Execute);

		// Act:
		notifyMosaicTransfer(context, context.recipient, context.recipient2, INITIAL_QUANTITY, NotificationTrigger.Execute);

		// Assert:
		context.assertMosaicIds(context.sender, true);
		context.assertMosaicIds(context.recipient, false);
		context.assertMosaicIds(context.recipient2, true);
	}

	@Test
	public void notifyUndoMosaicTransferWithPartialBalanceTransferUpdatesMosaicIds() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionToCache();
		notifyMosaicTransfer(context, new Quantity(250), NotificationTrigger.Execute);

		// Act:
		notifyMosaicTransfer(context, new Quantity(111), NotificationTrigger.Undo);

		// Assert:
		context.assertMosaicIds(context.sender, true);
		context.assertMosaicIds(context.recipient, true);
	}

	@Test
	public void notifyUndoMosaicTransferWithFullBalanceTransferFromOwnerUpdatesMosaicIds() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionToCache();
		notifyMosaicTransfer(context, INITIAL_QUANTITY, NotificationTrigger.Execute);

		// Act:
		notifyMosaicTransfer(context, context.recipient, context.sender, INITIAL_QUANTITY, NotificationTrigger.Undo);

		// Assert:
		context.assertMosaicIds(context.sender, true);
		context.assertMosaicIds(context.recipient, false);
		context.assertMosaicIds(context.recipient2, false);
	}

	@Test
	public void notifyUndoMosaicTransferWithFullBalanceTransferFromNonOwnerUpdatesMosaicIds() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaicDefinitionToCache();
		notifyMosaicTransfer(context, INITIAL_QUANTITY, NotificationTrigger.Execute);
		notifyMosaicTransfer(context, context.recipient, context.recipient2, INITIAL_QUANTITY, NotificationTrigger.Execute);

		// Act:
		notifyMosaicTransfer(context, context.recipient2, context.recipient, INITIAL_QUANTITY, NotificationTrigger.Undo);

		// Assert:
		context.assertMosaicIds(context.sender, true);
		context.assertMosaicIds(context.recipient, true);
		context.assertMosaicIds(context.recipient2, false);
	}

	// endregion

	// region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = context.createObserver();

		// Act:
		observer.notify(new BalanceTransferNotification(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		context.assertMosaicIds(context.sender, false);
		context.assertMosaicIds(context.recipient, false);
	}

	// endregion

	private void notifyMosaicDefinitionCreation(final TestContext context, final NotificationTrigger notificationTrigger) {
		// Arrange:
		final BlockTransactionObserver observer = context.createObserver();

		// Act:
		observer.notify(new MosaicDefinitionCreationNotification(context.mosaicDefinition),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private static void notifyMosaicTransfer(final TestContext context, final Quantity quantity,
			final NotificationTrigger notificationTrigger) {
		// Act:
		notifyMosaicTransfer(context, context.sender, context.recipient, quantity, notificationTrigger);
	}

	private static void notifyMosaicTransfer(final TestContext context, final Account sender, final Account recipient,
			final Quantity quantity, final NotificationTrigger notificationTrigger) {
		// Arrange:
		final BlockTransactionObserver observer = context.createObserver();

		// Act:
		observer.notify(new MosaicTransferNotification(sender, recipient, context.mosaicDefinition.getId(), quantity),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private static class TestContext {
		private final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(1, createMosaicProperties());
		private final Account sender = this.mosaicDefinition.getCreator();
		private final Account recipient = Utils.generateRandomAccount();
		private final Account recipient2 = Utils.generateRandomAccount();
		private final DefaultNamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		private final AccountStateCache accountStateCache = new DefaultAccountStateCache().copy();

		public TestContext() {
			final NamespaceId namespaceId = this.mosaicDefinition.getId().getNamespaceId();
			this.namespaceCache.add(new Namespace(namespaceId, this.mosaicDefinition.getCreator(), BlockHeight.ONE));
		}

		public void addMosaicDefinitionToCache() {
			final NamespaceId namespaceId = this.mosaicDefinition.getId().getNamespaceId();
			this.namespaceCache.get(namespaceId).getMosaics().add(this.mosaicDefinition);
		}

		private BlockTransactionObserver createObserver() {
			// note that this observer is dependent on MosaicDefinitionCreationObserver and MosaicTransferObserver
			final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
			builder.add(new MosaicDefinitionCreationObserver(this.namespaceCache));
			builder.add(new MosaicTransferObserver(this.namespaceCache));
			builder.add(new AccountInfoMosaicIdsObserver(this.namespaceCache, this.accountStateCache));
			return builder.build();
		}

		private void assertMosaicIds(final Account account, final boolean hasMosaicSubscription) {
			// Assert:
			final AccountInfo info = this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo();
			MatcherAssert.assertThat(info.getMosaicIds().size(), IsEqual.equalTo(hasMosaicSubscription ? 1 : 0));
			if (hasMosaicSubscription) {
				MatcherAssert.assertThat(info.getMosaicIds(),
						IsEquivalent.equivalentTo(Collections.singletonList(this.mosaicDefinition.getId())));
			}
		}

		private static MosaicProperties createMosaicProperties() {
			return Utils.createMosaicProperties(1000L, 4, null, true);
		}
	}
}
