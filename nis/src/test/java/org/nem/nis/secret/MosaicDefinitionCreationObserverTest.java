package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.MosaicEntry;
import org.nem.nis.test.NisUtils;

import java.util.Arrays;

public class MosaicDefinitionCreationObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 111;
	private static final long FORK_HEIGHT_MOSAIC_REDEFINITION = new BlockHeight(
			BlockMarkerConstants.MOSAIC_REDEFINITION_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24)).getRaw();
	private final long[] HEIGHTS_BEFORE_FORK = new long[]{
			1, 10, 100, 1000, FORK_HEIGHT_MOSAIC_REDEFINITION - 1
	};
	private final long[] HEIGHTS_AT_AND_AFTER_FORK = new long[]{
			FORK_HEIGHT_MOSAIC_REDEFINITION, FORK_HEIGHT_MOSAIC_REDEFINITION + 1, FORK_HEIGHT_MOSAIC_REDEFINITION + 10,
			FORK_HEIGHT_MOSAIC_REDEFINITION + 100000
	};

	// region mosaic creation

	@Test
	public void notifyExecuteCallsMosaicCacheAddWithExpectedMosaicDefinition() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Execute);

		// Assert:
		MatcherAssert.assertThat(context.getNumNamespaces(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(context.getNumMosaicDefinitions(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(context.cacheContainsMosaicDefinition(), IsEqual.equalTo(true));
	}

	@Test
	public void notifyUndoCallsMosaicCacheRemoveWithExpectedMosaicDefinition() {
		// Arrange:
		final TestContext context = new TestContext();
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Execute);

		// Act:
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Undo);

		// Assert:
		MatcherAssert.assertThat(context.getNumNamespaces(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(context.getNumMosaicDefinitions(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(context.cacheContainsMosaicDefinition(), IsEqual.equalTo(false));
	}

	// endregion

	// region mosaic definition change

	@Test
	public void notifyExecuteCreatesUntouchedMosaicEntryIfPropertiesChangedAtAnyHeight() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(7, Utils.createMosaicPropertiesWithInitialSupply(0L));

		// Assert: since the supply is 0 the balances are empty
		Arrays.stream(HEIGHTS_BEFORE_FORK).forEach(height -> assertMosaicRedefinitionBehavior(mosaicDefinition, height, 0L, 0));
		Arrays.stream(HEIGHTS_AT_AND_AFTER_FORK).forEach(height -> assertMosaicRedefinitionBehavior(mosaicDefinition, height, 0L, 0));
	}

	@Test
	public void notifyExecuteCreatesUntouchedMosaicEntryIfLevyChangedAtAnyHeight() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(7, Utils.createMosaicPropertiesWithInitialSupply(5L),
				Utils.createMosaicLevy());
		// Assert:
		Arrays.stream(HEIGHTS_BEFORE_FORK).forEach(height -> assertMosaicRedefinitionBehavior(mosaicDefinition, height, 5L, 1));
		Arrays.stream(HEIGHTS_AT_AND_AFTER_FORK).forEach(height -> assertMosaicRedefinitionBehavior(mosaicDefinition, height, 5L, 1));
	}

	@Test
	public void notifyExecuteCreatesUntouchedMosaicEntryIfOnlyDescriptorChangedBeforeFork() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(7, Utils.createMosaicPropertiesWithInitialSupply(5L),
				new MosaicDescriptor("This is a new description"));

		// Assert:
		Arrays.stream(HEIGHTS_BEFORE_FORK).forEach(height -> assertMosaicRedefinitionBehavior(mosaicDefinition, height, 5L, 1));
	}

	@Test
	public void notifyExecuteCreatesMosaicEntryWithInheritedDataIfOnlyDescriptorChangedAtAndAfterFork() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(7, Utils.createMosaicPropertiesWithInitialSupply(5L),
				new MosaicDescriptor("This is a new description"));

		// Assert:
		Arrays.stream(HEIGHTS_AT_AND_AFTER_FORK).forEach(height -> assertMosaicRedefinitionBehavior(mosaicDefinition, height, 15L, 2));
	}

	private void assertMosaicRedefinitionBehavior(final MosaicDefinition mosaicDefinition, final long height, final long expectedSupply,
			final int expectedBalancesSize) {
		// Arrange: initial supply is 5
		final TestContext context = new TestContext();
		this.notifyMosaicDefinitionCreation(context, NotificationTrigger.Execute);
		final Address address = Utils.generateRandomAddress();
		context.increaseSupply(10L);
		context.incrementBalance(address, 8L);

		// Sanity:
		assertMosaicEntry(context, 15L, 2);

		// Act:
		this.notifyMosaicDefinitionCreation(context, mosaicDefinition, height, NotificationTrigger.Execute);

		// Assert:
		assertMosaicEntry(context, expectedSupply, expectedBalancesSize);
	}

	private static void assertMosaicEntry(final TestContext context, final Long supply, final int numBalances) {
		MosaicEntry entry = context.getMosaicEntry();
		MatcherAssert.assertThat(entry.getSupply(), IsEqual.equalTo(new Supply(supply)));
		MatcherAssert.assertThat(entry.getBalances().size(), IsEqual.equalTo(numBalances));
	}

	// endregion

	// region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicDefinitionCreationObserver observer = context.createObserver();

		// Act:
		observer.notify(new BalanceTransferNotification(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		MatcherAssert.assertThat(context.getNumNamespaces(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(context.getNumMosaicDefinitions(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(context.cacheContainsMosaicDefinition(), IsEqual.equalTo(false));
	}

	// endregion

	private void notifyMosaicDefinitionCreation(final TestContext context, final NotificationTrigger notificationTrigger) {
		// Act:
		notifyMosaicDefinitionCreation(context, context.mosaicDefinition, NOTIFY_BLOCK_HEIGHT, notificationTrigger);
	}

	private void notifyMosaicDefinitionCreation(final TestContext context, final MosaicDefinition mosaicDefinition, final long height,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final MosaicDefinitionCreationObserver observer = context.createObserver();

		// Act:
		observer.notify(new MosaicDefinitionCreationNotification(mosaicDefinition),
				NisUtils.createBlockNotificationContext(new BlockHeight(height), notificationTrigger));
	}

	private class TestContext {
		private final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(7,
				Utils.createMosaicPropertiesWithInitialSupply(5L));
		private final NamespaceCache namespaceCache = new DefaultNamespaceCache().copy();

		public TestContext() {
			this.namespaceCache.add(
					new Namespace(this.mosaicDefinition.getId().getNamespaceId(), this.mosaicDefinition.getCreator(), BlockHeight.ONE));
		}

		public int getNumNamespaces() {
			return this.namespaceCache.size();
		}

		public int getNumMosaicDefinitions() {
			return this.namespaceCache.get(this.mosaicDefinition.getId().getNamespaceId()).getMosaics().size();
		}

		public boolean cacheContainsMosaicDefinition() {
			return this.namespaceCache.get(this.mosaicDefinition.getId().getNamespaceId()).getMosaics()
					.contains(this.mosaicDefinition.getId());
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
