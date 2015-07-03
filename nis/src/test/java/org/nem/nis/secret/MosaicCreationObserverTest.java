package org.nem.nis.secret;

import org.junit.*;
import org.mockito.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.test.NisUtils;

public class MosaicCreationObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 111;

	//region mosaic creation

	@Test
	public void notifyExecuteCallsMosaicCacheAddWithExpectedMosaic() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyMosaicCreation(context, NotificationTrigger.Execute);

		// Assert:
		Mockito.verify(context.mosaicCache, Mockito.only()).add(context.mosaic);
	}

	@Test
	public void notifyUndoCallsMosaicCacheRemoveWithExpectedMosaic() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyMosaicCreation(context, NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.mosaicCache, Mockito.only()).remove(context.mosaic);
	}

	//endregion

	//region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicCreationObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new BalanceTransferNotification(
						Utils.generateRandomAccount(),
						Utils.generateRandomAccount(),
						Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.mosaicCache, Mockito.never()).add(Mockito.any());
		Mockito.verify(context.mosaicCache, Mockito.never()).remove(Mockito.any());
	}

	//endregion

	private void notifyMosaicCreation(
			final TestContext context,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final MosaicCreationObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new MosaicCreationNotification(context.mosaic),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private class TestContext {
		private final Mosaic mosaic = Mockito.mock(Mosaic.class);
		private final MosaicCache mosaicCache = Mockito.mock(MosaicCache.class);

		private MosaicCreationObserver createObserver() {
			return new MosaicCreationObserver(this.mosaicCache);
		}
	}
}