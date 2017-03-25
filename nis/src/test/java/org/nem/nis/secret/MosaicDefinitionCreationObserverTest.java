package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.state.MosaicEntry;
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

	//region mosaic definition change

	@Test
	public void notifyExecuteCreatesUntouchedMosaicEntryIfPropertiesChanged() {
		// Arrange: initial supply is 5
		final TestContext context = new TestContext();
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(7, Utils.createMosaicPropertiesWithInitialSupply(0L));
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Execute);

		// Sanity:
		assertEntry(context, 5L, 1);

		// Act:
		this.notifyMosaicDefinitionCreation(context, mosaicDefinition, NotificationTrigger.Execute);

		// Assert: since the supply is 0 the balances are empty
		assertEntry(context, 0L, 0);
	}

	@Test
	public void notifyExecuteCreatesUntouchedMosaicEntryIfLevyChanged() {
		// Arrange: initial supply is 5
		final TestContext context = new TestContext();
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(
				7,
				Utils.createMosaicPropertiesWithInitialSupply(5L),
				Utils.createMosaicLevy());
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Execute);
		context.increaseSupply(10L);

		// Sanity:
		assertEntry(context, 15L, 1);

		// Act:
		this.notifyMosaicDefinitionCreation(context, mosaicDefinition, NotificationTrigger.Execute);

		// Assert:
		assertEntry(context, 5L, 1);
	}

	@Test
	public void notifyExecuteCreatesMosaicEntryWithInheritedDataIfOnlyDescriptorChanged() {
		// Arrange: initial supply is 5
		final TestContext context = new TestContext();
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(
				7,
				Utils.createMosaicPropertiesWithInitialSupply(5L),
				new MosaicDescriptor("This is a new description"));
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Execute);
		final Address address = Utils.generateRandomAddress();
		context.increaseSupply(10L);
		context.incrementBalance(address, 8L);

		// Sanity:
		assertEntry(context, 15L, 2);

		// Act:
		this.notifyMosaicDefinitionCreation(context, mosaicDefinition, NotificationTrigger.Execute);

		// Assert:
		assertEntry(context, 15L, 2);
	}

	private static void assertEntry(final TestContext context, final Long supply, final int numBalances) {
		MosaicEntry entry = context.getMosaicEntry();
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(new Supply(supply)));
		Assert.assertThat(entry.getBalances().size(), IsEqual.equalTo(numBalances));
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
		// Act:
		notifyMosaicDefinitionCreation(context, context.mosaicDefinition, notificationTrigger);
	}

	private void notifyMosaicDefinitionCreation(
			final TestContext context,
			final MosaicDefinition mosaicDefinition,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final MosaicDefinitionCreationObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new MosaicDefinitionCreationNotification(mosaicDefinition),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private class TestContext {
		private final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(7, Utils.createMosaicPropertiesWithInitialSupply(5L));
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

		public MosaicEntry getMosaicEntry() {
			return this.namespaceCache.get(this.mosaicDefinition.getId().getNamespaceId()).getMosaics().get(this.mosaicDefinition.getId());
		}

		public void increaseSupply(final Long delta) {
			getMosaicEntry().increaseSupply(new Supply(delta));
		}

		public void incrementBalance(final Address address, final Long increase) {
			getMosaicEntry().getBalances().incrementBalance(address, Quantity.fromValue(increase));
		}

		public MosaicDefinitionCreationObserver createObserver() {
			return new MosaicDefinitionCreationObserver(this.namespaceCache);
		}
	}
}