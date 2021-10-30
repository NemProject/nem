package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
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

public class MosaicTransferObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 111;

	// region supply change

	@Test
	public void notifyExecuteTransfersFromSenderToRecipient() {

		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		notifyMosaicTransfer(context, new Quantity(250), NotificationTrigger.Execute);

		// Assert:
		context.assertBalances(new Quantity(9750), new Quantity(250));
	}

	@Test
	public void notifyUndoTransfersFromSenderToRecipient() {
		// Arrange:
		final TestContext context = new TestContext();
		notifyMosaicTransfer(context, new Quantity(250), NotificationTrigger.Execute);

		// Act:
		notifyMosaicTransfer(context, new Quantity(111), NotificationTrigger.Undo);

		// Assert:
		context.assertBalances(new Quantity(9750 - 111), new Quantity(250 + 111));
	}

	// endregion

	// region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicTransferObserver observer = context.createObserver();

		// Act:
		observer.notify(new BalanceTransferNotification(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		context.assertBalances(new Quantity(10000), Quantity.ZERO);
	}

	// endregion

	private static void notifyMosaicTransfer(final TestContext context, final Quantity quantity,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final MosaicTransferObserver observer = context.createObserver();

		// Act:
		observer.notify(new MosaicTransferNotification(context.sender, context.recipient, context.mosaicDefinition.getId(), quantity),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private static class TestContext {
		private final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(1, createMosaicProperties());
		private final Account sender = this.mosaicDefinition.getCreator();
		private final Account recipient = Utils.generateRandomAccount();
		private final DefaultNamespaceCache namespaceCache = new DefaultNamespaceCache().copy();

		private TestContext() {
			final NamespaceId namespaceId = this.mosaicDefinition.getId().getNamespaceId();
			this.namespaceCache.add(new Namespace(namespaceId, this.mosaicDefinition.getCreator(), BlockHeight.ONE));
			this.namespaceCache.get(namespaceId).getMosaics().add(this.mosaicDefinition);
		}

		private MosaicTransferObserver createObserver() {
			return new MosaicTransferObserver(this.namespaceCache);
		}

		private void assertBalances(final Quantity senderBalance, final Quantity recipientBalance) {
			// Assert:
			// - single namespace
			MatcherAssert.assertThat(this.namespaceCache.size(), IsEqual.equalTo(2));

			// - single mosaic mosaic
			final Mosaics mosaics = this.namespaceCache.get(this.mosaicDefinition.getId().getNamespaceId()).getMosaics();
			MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(1));

			// - correct balances
			final int numExpectedBalances = isNonZero(senderBalance) + isNonZero(recipientBalance);
			final MosaicEntry entry = mosaics.get(this.mosaicDefinition.getId());
			MatcherAssert.assertThat(entry.getBalances().size(), IsEqual.equalTo(numExpectedBalances));
			MatcherAssert.assertThat(entry.getBalances().getBalance(this.sender.getAddress()), IsEqual.equalTo(senderBalance));
			MatcherAssert.assertThat(entry.getBalances().getBalance(this.recipient.getAddress()), IsEqual.equalTo(recipientBalance));
		}

		private static int isNonZero(final Quantity quantity) {
			return Quantity.ZERO.equals(quantity) ? 0 : 1;
		}

		private static MosaicProperties createMosaicProperties() {
			return Utils.createMosaicProperties(1000L, 1, null, true);
		}
	}
}
