package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.namespace.Namespace;
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
		Assert.assertThat(context.getNumNamespaces(), IsEqual.equalTo(1 + 1));
		Assert.assertThat(context.getNumMosaics(), IsEqual.equalTo(1));
		Assert.assertThat(context.cacheContainsMosaic(), IsEqual.equalTo(true));
	}

	@Test
	public void notifyUndoCallsMosaicCacheRemoveWithExpectedMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		this.notifyMosaicCreation(context, NotificationTrigger.Execute);

		// Act:
		this.notifyMosaicCreation(context, NotificationTrigger.Undo);

		// Assert:
		Assert.assertThat(context.getNumNamespaces(), IsEqual.equalTo(1 + 1));
		Assert.assertThat(context.getNumMosaics(), IsEqual.equalTo(0));
		Assert.assertThat(context.cacheContainsMosaic(), IsEqual.equalTo(false));
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
		Assert.assertThat(context.getNumNamespaces(), IsEqual.equalTo(1 + 1));
		Assert.assertThat(context.getNumMosaics(), IsEqual.equalTo(0));
		Assert.assertThat(context.cacheContainsMosaic(), IsEqual.equalTo(false));
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
		private final Mosaic mosaic = Utils.createMosaic(7);
		private final NamespaceCache namespaceCache = new DefaultNamespaceCache();

		public TestContext() {
			this.namespaceCache.add(new Namespace(this.mosaic.getId().getNamespaceId(), this.mosaic.getCreator(), BlockHeight.ONE));
		}

		public int getNumNamespaces() {
			return this.namespaceCache.size();
		}

		public int getNumMosaics() {
			return this.namespaceCache.get(this.mosaic.getId().getNamespaceId()).getMosaics().size();
		}

		public boolean cacheContainsMosaic() {
			return this.namespaceCache.get(this.mosaic.getId().getNamespaceId()).getMosaics().contains(this.mosaic.getId());
		}

		public MosaicCreationObserver createObserver() {
			return new MosaicCreationObserver(this.namespaceCache);
		}
	}
}