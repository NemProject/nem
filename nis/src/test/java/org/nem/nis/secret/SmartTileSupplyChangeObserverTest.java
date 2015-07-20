package org.nem.nis.secret;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

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
		notifySmartTileSupplyChange(context, supplyType, trigger);

		// Assert:
		context.assertMosaicEntryLookupDelegation();
		Mockito.verify(context.mosaicEntry, Mockito.only()).increaseSupply(Quantity.fromValue(123));
	}

	private static void assertSupplyDecrease(final NotificationTrigger trigger, final SmartTileSupplyType supplyType) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		notifySmartTileSupplyChange(context, supplyType, trigger);

		// Assert:
		context.assertMosaicEntryLookupDelegation();
		Mockito.verify(context.mosaicEntry, Mockito.only()).decreaseSupply(Quantity.fromValue(123));
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
		Mockito.verify(context.namespaceCache, Mockito.never()).get(Mockito.any());
	}

	//endregion

	private static void notifySmartTileSupplyChange(
			final TestContext context,
			final SmartTileSupplyType supplyType,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final SmartTileSupplyChangeObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new SmartTileSupplyChangeNotification(context.supplier, context.smartTile, supplyType),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private static class TestContext {
		private final Account supplier = Utils.generateRandomAccount();
		private final NamespaceId namespaceId = new NamespaceId("foo");
		private final MosaicId mosaicId = new MosaicId(this.namespaceId, "bar");
		private final SmartTile smartTile = new SmartTile(this.mosaicId, Quantity.fromValue(123));
		private final NamespaceCache namespaceCache = Mockito.mock(NamespaceCache.class);
		private final NamespaceEntry namespaceEntry = Mockito.mock(NamespaceEntry.class);
		private final Mosaics mosaics = Mockito.mock(Mosaics.class);
		private final MosaicEntry mosaicEntry = Mockito.mock(MosaicEntry.class);

		private TestContext() {
			Mockito.when(this.namespaceCache.get(this.namespaceId)).thenReturn(this.namespaceEntry);
			Mockito.when(this.namespaceEntry.getMosaics()).thenReturn(this.mosaics);
			Mockito.when(this.mosaics.get(this.mosaicId)).thenReturn(this.mosaicEntry);
		}

		private SmartTileSupplyChangeObserver createObserver() {
			return new SmartTileSupplyChangeObserver(this.namespaceCache);
		}

		private void assertMosaicEntryLookupDelegation() {
			Mockito.verify(this.namespaceCache, Mockito.only()).get(this.namespaceId);
			Mockito.verify(this.namespaceEntry, Mockito.only()).getMosaics();
			Mockito.verify(this.mosaics, Mockito.only()).get(this.mosaicId);
		}
	}
}
