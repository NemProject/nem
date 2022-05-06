package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

public class MosaicSupplyChangeObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 111;

	// region supply change

	@Test
	public void notifyExecuteCreateMosaicIncreasesSupply() {
		// Assert:
		assertSupplyIncrease(NotificationTrigger.Execute, MosaicSupplyType.Create);
	}

	@Test
	public void notifyExecuteDeleteMosaicDecreasesSupply() {
		// Assert:
		assertSupplyDecrease(NotificationTrigger.Execute, MosaicSupplyType.Delete);
	}

	@Test
	public void notifyUndoCreateMosaicDecreasesSupply() {
		// Assert:
		assertSupplyDecrease(NotificationTrigger.Undo, MosaicSupplyType.Create);
	}

	@Test
	public void notifyUndoDeleteMosaicIncreasesSupply() {
		// Assert:
		assertSupplyIncrease(NotificationTrigger.Undo, MosaicSupplyType.Delete);
	}

	private static void assertSupplyIncrease(final NotificationTrigger trigger, final MosaicSupplyType supplyType) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		notifyMosaicSupplyChange(context, new Supply(123), supplyType, trigger);

		// Assert:
		context.assertSupply(new Supply(1123));
	}

	private static void assertSupplyDecrease(final NotificationTrigger trigger, final MosaicSupplyType supplyType) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		notifyMosaicSupplyChange(context, new Supply(123), supplyType, trigger);

		// Assert:
		context.assertSupply(new Supply(877));
	}

	@Test
	public void DecreasingCreatorBalanceToZeroRemovesMosaicIdFromAccountState() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicSupplyChangeObserver observer = context.createObserver();
		Mockito.when(context.accountStateCache.findStateByAddress(context.accountState.getAddress())).thenReturn(context.accountState);
		context.accountState.getAccountInfo().getMosaicIds().add(context.mosaicDefinition.getId());

		// decrease the creator balance by 100 so it doesn't match supply
		final Mosaics mosaics = context.namespaceCache.get(context.mosaicDefinition.getId().getNamespaceId()).getMosaics();
		final MosaicEntry entry = mosaics.get(context.mosaicDefinition.getId());
		entry.getBalances().decrementBalance(context.supplier.getAddress(), MosaicUtils.toQuantity(new Supply(100), 4));

		// Act:
		notifyMosaicSupplyChange(context, new Supply(900), MosaicSupplyType.Delete, NotificationTrigger.Execute);

		// Assert: mosaic id must have been removed
		MatcherAssert.assertThat(context.accountState.getAccountInfo().getMosaicIds().isEmpty(), IsEqual.equalTo(true));
	}

	// endregion

	// region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicSupplyChangeObserver observer = context.createObserver();

		// Act:
		observer.notify(new BalanceTransferNotification(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		context.assertSupply(new Supply(1000));
	}

	// endregion

	private static void notifyMosaicSupplyChange(final TestContext context, final Supply delta, final MosaicSupplyType supplyType,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final MosaicSupplyChangeObserver observer = context.createObserver();

		// Act:
		observer.notify(new MosaicSupplyChangeNotification(context.supplier, context.mosaicDefinition.getId(), delta, supplyType),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private static class TestContext {
		private final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(1, createMosaicProperties());
		private final Account supplier = this.mosaicDefinition.getCreator();
		private final DefaultNamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final AccountState accountState = new AccountState(this.supplier.getAddress());

		private TestContext() {
			final NamespaceId namespaceId = this.mosaicDefinition.getId().getNamespaceId();
			this.namespaceCache.add(new Namespace(namespaceId, this.mosaicDefinition.getCreator(), BlockHeight.ONE));
			this.namespaceCache.get(namespaceId).getMosaics().add(this.mosaicDefinition);
		}

		private MosaicSupplyChangeObserver createObserver() {
			return new MosaicSupplyChangeObserver(this.namespaceCache, this.accountStateCache);
		}

		private void assertSupply(final Supply supply) {
			// - single namespace
			MatcherAssert.assertThat(this.namespaceCache.size(), IsEqual.equalTo(2));

			// - single mosaic
			final Mosaics mosaics = this.namespaceCache.get(this.mosaicDefinition.getId().getNamespaceId()).getMosaics();
			MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(1));

			// - correct supply
			final MosaicEntry entry = mosaics.get(this.mosaicDefinition.getId());
			MatcherAssert.assertThat(entry.getSupply(), IsEqual.equalTo(supply));

			// - correct balance
			final Quantity expectedBalance = MosaicUtils.toQuantity(supply, 4);
			MatcherAssert.assertThat(entry.getBalances().size(), IsEqual.equalTo(1));
			MatcherAssert.assertThat(entry.getBalances().getBalance(this.supplier.getAddress()), IsEqual.equalTo(expectedBalance));
		}

		private static MosaicProperties createMosaicProperties() {
			return Utils.createMosaicProperties(1000L, 4, null, true);
		}
	}
}
