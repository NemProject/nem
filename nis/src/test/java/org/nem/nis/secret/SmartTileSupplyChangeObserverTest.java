package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

import java.util.Properties;

public class SmartTileSupplyChangeObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 111;

	//region supply change

	@Test
	public void notifyExecuteCreateSmartTileIncreasesSupply() {
		// Assert:
		assertSupplyIncrease(NotificationTrigger.Execute, SmartTileSupplyType.CreateSmartTiles);
	}

	@Test
	public void notifyExecuteDeleteSmartTileDecreasesSupply() {
		// Assert:
		assertSupplyDecrease(NotificationTrigger.Execute, SmartTileSupplyType.DeleteSmartTiles);
	}

	@Test
	public void notifyUndoCreateSmartTileDecreasesSupply() {
		// Assert:
		assertSupplyDecrease(NotificationTrigger.Undo, SmartTileSupplyType.CreateSmartTiles);
	}

	@Test
	public void notifyUndoDeleteSmartTileIncreasesSupply() {
		// Assert:
		assertSupplyIncrease(NotificationTrigger.Undo, SmartTileSupplyType.DeleteSmartTiles);
	}

	private static void assertSupplyIncrease(final NotificationTrigger trigger, final SmartTileSupplyType supplyType) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		notifySmartTileSupplyChange(context, new Supply(123), supplyType, trigger);

		// Assert:
		context.assertSupply(new Supply(1123));
	}

	private static void assertSupplyDecrease(final NotificationTrigger trigger, final SmartTileSupplyType supplyType) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		notifySmartTileSupplyChange(context, new Supply(123), supplyType, trigger);

		// Assert:
		context.assertSupply(new Supply(877));
	}

	//endregion

	//region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final SmartTileSupplyChangeObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new BalanceTransferNotification(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		context.assertSupply(new Supply(1000));
	}

	//endregion

	private static void notifySmartTileSupplyChange(
			final TestContext context,
			final Supply delta,
			final SmartTileSupplyType supplyType,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final SmartTileSupplyChangeObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new SmartTileSupplyChangeNotification(context.supplier, context.mosaic.getId(), delta, supplyType),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private static class TestContext {
		private final Mosaic mosaic = Utils.createMosaic(1, createMosaicProperties());
		private final Account supplier = this.mosaic.getCreator();
		private final DefaultNamespaceCache namespaceCache = new DefaultNamespaceCache();

		private TestContext() {
			final NamespaceId namespaceId = this.mosaic.getId().getNamespaceId();
			this.namespaceCache.add(new Namespace(namespaceId, this.mosaic.getCreator(), BlockHeight.ONE));
			this.namespaceCache.get(namespaceId).getMosaics().add(this.mosaic);
		}

		private SmartTileSupplyChangeObserver createObserver() {
			return new SmartTileSupplyChangeObserver(this.namespaceCache);
		}

		private void assertSupply(final Supply supply) {
			// - single namespace
			Assert.assertThat(this.namespaceCache.size(), IsEqual.equalTo(2));

			// - single mosaic
			final Mosaics mosaics = this.namespaceCache.get(this.mosaic.getId().getNamespaceId()).getMosaics();
			Assert.assertThat(mosaics.size(), IsEqual.equalTo(1));

			// - correct supply
			final MosaicEntry entry = mosaics.get(this.mosaic.getId());
			Assert.assertThat(entry.getSupply(), IsEqual.equalTo(supply));

			// - correct balance
			final Quantity expectedBalance = MosaicUtils.toQuantity(supply, 4);
			Assert.assertThat(entry.getBalances().size(), IsEqual.equalTo(1));
			Assert.assertThat(entry.getBalances().getBalance(this.supplier.getAddress()), IsEqual.equalTo(expectedBalance));
		}

		private static MosaicProperties createMosaicProperties() {
			return Utils.createMosaicProperties(1000L, 4, null, true);
		}
	}
}
