package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.DefaultNamespaceCache;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

public class SmartTileTransferObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 111;

	//region supply change

	@Test
	public void notifyExecuteTransfersFromSenderToRecipient() {

		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		notifySmartTileTransfer(context, new Quantity(250), NotificationTrigger.Execute);

		// Assert:
		context.assertBalances(new Quantity(9750), new Quantity(250));
	}

	@Test
	public void notifyUndoTransfersFromRecipientToSender() {
		// Arrange:
		final TestContext context = new TestContext();
		notifySmartTileTransfer(context, new Quantity(250), NotificationTrigger.Execute);

		// Act:
		notifySmartTileTransfer(context, new Quantity(111), NotificationTrigger.Undo);

		// Assert:
		context.assertBalances(new Quantity(9861), new Quantity(139));
	}

	//endregion

	//region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final SmartTileTransferObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new BalanceTransferNotification(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		context.assertBalances(new Quantity(10000), Quantity.ZERO);
	}

	//endregion

	private static void notifySmartTileTransfer(
			final TestContext context,
			final Quantity quantity,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final SmartTileTransferObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new SmartTileTransferNotification(context.sender, context.recipient, context.mosaicDefinition.getId(), quantity),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private static class TestContext {
		private final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(1, createMosaicProperties());
		private final Account sender = this.mosaicDefinition.getCreator();
		private final Account recipient = Utils.generateRandomAccount();
		private final DefaultNamespaceCache namespaceCache = new DefaultNamespaceCache();

		private TestContext() {
			final NamespaceId namespaceId = this.mosaicDefinition.getId().getNamespaceId();
			this.namespaceCache.add(new Namespace(namespaceId, this.mosaicDefinition.getCreator(), BlockHeight.ONE));
			this.namespaceCache.get(namespaceId).getMosaics().add(this.mosaicDefinition);
		}

		private SmartTileTransferObserver createObserver() {
			return new SmartTileTransferObserver(this.namespaceCache);
		}

		private void assertBalances(final Quantity senderBalance, final Quantity recipientBalance) {
			// Assert:
			// - single namespace
			Assert.assertThat(this.namespaceCache.size(), IsEqual.equalTo(2));

			// - single mosaic mosaic
			final Mosaics mosaics = this.namespaceCache.get(this.mosaicDefinition.getId().getNamespaceId()).getMosaics();
			Assert.assertThat(mosaics.size(), IsEqual.equalTo(1));

			// - correct balances
			final int numExpectedBalances = isNonZero(senderBalance) + isNonZero(recipientBalance);
			final MosaicEntry entry = mosaics.get(this.mosaicDefinition.getId());
			Assert.assertThat(entry.getBalances().size(), IsEqual.equalTo(numExpectedBalances));
			Assert.assertThat(entry.getBalances().getBalance(this.sender.getAddress()), IsEqual.equalTo(senderBalance));
			Assert.assertThat(entry.getBalances().getBalance(this.recipient.getAddress()), IsEqual.equalTo(recipientBalance));
		}

		private static int isNonZero(final Quantity quantity) {
			return Quantity.ZERO.equals(quantity) ? 0 : 1;
		}

		private static MosaicProperties createMosaicProperties() {
			return Utils.createMosaicProperties(1000L, 1, null, true);
		}
	}
}