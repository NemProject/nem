package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.test.NisUtils;

public class MosaicDefinitionCreationObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 111;

	//region mosaic creation

	@Test
	public void notifyExecuteCallsMosaicCacheAddWithExpectedMosaicDefinition() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Execute);

		// Assert:
		Assert.assertThat(context.getNumNamespaces(), IsEqual.equalTo(1 + 1));
		Assert.assertThat(context.getNumMosaicDefinitions(), IsEqual.equalTo(1));
		Assert.assertThat(context.cacheContainsMosaicDefinition(), IsEqual.equalTo(true));
	}

	@Test
	public void notifyUndoCallsMosaicCacheRemoveWithExpectedMosaicDefinition() {
		// Arrange:
		final TestContext context = new TestContext();
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Execute);

		// Act:
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Undo);

		// Assert:
		Assert.assertThat(context.getNumNamespaces(), IsEqual.equalTo(1 + 1));
		Assert.assertThat(context.getNumMosaicDefinitions(), IsEqual.equalTo(0));
		Assert.assertThat(context.cacheContainsMosaicDefinition(), IsEqual.equalTo(false));
	}

	//endregion

	//region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicDefinitionCreationObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new BalanceTransferNotification(
						Utils.generateRandomAccount(),
						Utils.generateRandomAccount(),
						Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Assert.assertThat(context.getNumNamespaces(), IsEqual.equalTo(1 + 1));
		Assert.assertThat(context.getNumMosaicDefinitions(), IsEqual.equalTo(0));
		Assert.assertThat(context.cacheContainsMosaicDefinition(), IsEqual.equalTo(false));
	}

	//endregion

	private void notifyMosaicDefinitionCreation(
			final TestContext context,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final MosaicDefinitionCreationObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new MosaicDefinitionCreationNotification(context.mosaicDefinition),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private class TestContext {
		private final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(7);
		private final NamespaceCache namespaceCache = new DefaultNamespaceCache().copy();

		public TestContext() {
			this.namespaceCache.add(new Namespace(this.mosaicDefinition.getId().getNamespaceId(), this.mosaicDefinition.getCreator(), BlockHeight.ONE));
		}

		public int getNumNamespaces() {
			return this.namespaceCache.size();
		}

		public int getNumMosaicDefinitions() {
			return this.namespaceCache.get(this.mosaicDefinition.getId().getNamespaceId()).getMosaics().size();
		}

		public boolean cacheContainsMosaicDefinition() {
			return this.namespaceCache.get(this.mosaicDefinition.getId().getNamespaceId()).getMosaics().contains(this.mosaicDefinition.getId());
		}

		public MosaicDefinitionCreationObserver createObserver() {
			return new MosaicDefinitionCreationObserver(this.namespaceCache);
		}
	}
}