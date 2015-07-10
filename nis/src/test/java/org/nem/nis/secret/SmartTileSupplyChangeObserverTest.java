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
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

public class SmartTileSupplyChangeObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 111;

	//region supply change

	@Test
	public void notifyExecuteCreateSmartTileCallsSmartTileMapAddWithExpectedSmartTile() {
		// Assert:
		assertObserverBehavior(NotificationTrigger.Execute, SmartTileSupplyType.CreateSmartTiles, true);
	}

	@Test
	public void notifyExecuteDeleteSmartTileCallsSmartTileMapSubtractWithExpectedSmartTile() {
		// Assert:
		assertObserverBehavior(NotificationTrigger.Execute, SmartTileSupplyType.DeleteSmartTiles, false);
	}

	@Test
	public void notifyUndoCreateSmartTileCallsSmartTileMapSubtractWithExpectedSmartTile() {
		// Assert:
		assertObserverBehavior(NotificationTrigger.Undo, SmartTileSupplyType.CreateSmartTiles, false);
	}

	@Test
	public void notifyUndoDeleteSmartTileCallsSmartTileMapAddWithExpectedSmartTile() {
		// Assert:
		assertObserverBehavior(NotificationTrigger.Undo, SmartTileSupplyType.DeleteSmartTiles, true);
	}

	private void assertObserverBehavior(
			final NotificationTrigger trigger,
			final SmartTileSupplyType supplyType,
			final boolean shouldContainSmartTile) {
		// Arrange:
		final TestContext context = new TestContext();
		if (!shouldContainSmartTile) {
			context.smartTileMap.add(context.smartTile);
		}

		// Act:
		this.notifySmartTileSupplyChange(context, supplyType, trigger);
		final SmartTile smartTile = context.smartTileMap.get(context.smartTile.getMosaicId());

		// Assert:
		Mockito.verify(context.accountStateCache, Mockito.only()).findStateByAddress(context.supplier.getAddress());
		Mockito.verify(context.accountState, Mockito.only()).getSmartTileMap();
		Assert.assertThat(smartTile, shouldContainSmartTile ? IsEqual.equalTo(context.smartTile) : IsNull.nullValue());
		if (shouldContainSmartTile) {
			Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123)));
		}
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
				new BalanceTransferNotification(
						Utils.generateRandomAccount(),
						Utils.generateRandomAccount(),
						Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.accountStateCache, Mockito.never()).findStateByAddress(Mockito.any());
	}

	//endregion

	private void notifySmartTileSupplyChange(
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

	private class TestContext {
		private final Account supplier = Utils.generateRandomAccount();
		private final MosaicId mosaicId = new MosaicId(new NamespaceId("foo"), "bar");
		private final SmartTile smartTile = new SmartTile(this.mosaicId, Quantity.fromValue(123));
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final AccountState accountState = Mockito.mock(AccountState.class);
		private final SmartTileMap smartTileMap = new SmartTileMap();

		private TestContext() {
			Mockito.when(this.accountStateCache.findStateByAddress(this.supplier.getAddress())).thenReturn(this.accountState);
			Mockito.when(this.accountState.getSmartTileMap()).thenReturn(this.smartTileMap);
		}

		private SmartTileSupplyChangeObserver createObserver() {
			return new SmartTileSupplyChangeObserver(this.accountStateCache);
		}
	}
}
