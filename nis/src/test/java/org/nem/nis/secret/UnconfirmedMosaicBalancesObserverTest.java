package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.MosaicTransferNotification;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.state.MosaicEntry;

public class UnconfirmedMosaicBalancesObserverTest {

	// region balance adjustment

	@Test
	public void notifyDebitsSenderAndCreditsRecipient() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(context.createMosaicTransferNotification(Quantity.fromValue(10)));

		// Assert:
		Assert.assertThat(context.observer.get(context.sender, context.mosaicId), IsEqual.equalTo(Quantity.fromValue(990)));
		Assert.assertThat(context.observer.get(context.recipient, context.mosaicId), IsEqual.equalTo(Quantity.fromValue(10)));
	}

	@Test
	public void notifyCumulativelyAdjustsUnconfirmedMosaicBalance() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(context.createMosaicTransferNotification(Quantity.fromValue(10)));
		context.observer.notify(context.createMosaicTransferNotification(Quantity.fromValue(7)));
		context.observer.notify(context.createMosaicTransferNotification(Quantity.fromValue(23)));
		context.observer.notify(context.createMosaicTransferNotification(Quantity.fromValue(100)));

		// Assert:
		Assert.assertThat(context.observer.get(context.sender, context.mosaicId), IsEqual.equalTo(Quantity.fromValue(860)));
		Assert.assertThat(context.observer.get(context.recipient, context.mosaicId), IsEqual.equalTo(Quantity.fromValue(140)));
	}

	// endregion

	// region clearCache

	@Test
	public void clearCacheClearsMap() {
		// Arrange:
		final TestContext context = new TestContext();
		context.observer.notify(context.createMosaicTransferNotification(Quantity.fromValue(10)));

		// sanity check
		Assert.assertThat(context.observer.get(context.sender, context.mosaicId), IsEqual.equalTo(Quantity.fromValue(990)));
		Assert.assertThat(context.observer.get(context.recipient, context.mosaicId), IsEqual.equalTo(Quantity.fromValue(10)));

		// Act:
		context.observer.clearCache();

		// Assert:
		Assert.assertThat(context.observer.get(context.sender, context.mosaicId), IsEqual.equalTo(Quantity.fromValue(1000)));
		Assert.assertThat(context.observer.get(context.recipient, context.mosaicId), IsEqual.equalTo(Quantity.ZERO));
	}

	// endregion

	// region balances are valid

	@Test
	public void unconfirmedMosaicBalancesAreValidReturnsTrueIfAllUnconfirmedMosaicBalancesAreNonNegative() {
		// Arrange (sender has quantity 1000, recipient has quantity 0):
		final TestContext context = new TestContext();

		// Assert:
		Assert.assertThat(context.observer.unconfirmedMosaicBalancesAreValid(), IsEqual.equalTo(true));
	}

	@Test
	public void unconfirmedMosaicBalancesAreValidReturnsFalseIfAtLeastOnelUnconfirmedMosaicBalanceAreIsNegative() {
		// Arrange (initially sender has quantity 1000, recipient has quantity 0):
		final TestContext context = new TestContext();
		context.observer.notify(context.createMosaicTransferNotification(Quantity.fromValue(600)));
		context.decrementMosaicBalance(context.sender, Quantity.fromValue(500));

		// Assert:
		Assert.assertThat(context.observer.unconfirmedMosaicBalancesAreValid(), IsEqual.equalTo(false));
	}

	// endregion

	private static class TestContext {
		private final Account sender = new Account(Utils.generateRandomAddress());
		private final Account recipient = new Account(Utils.generateRandomAddress());
		private final MosaicId mosaicId = Utils.createMosaicId(1);
		private final NamespaceCache namespaceCache = new DefaultNamespaceCache();
		private final UnconfirmedMosaicBalancesObserver observer = Mockito.spy(new UnconfirmedMosaicBalancesObserver(this.namespaceCache));

		public TestContext() {
			this.setupCache(Supply.fromValue(1));
		}

		private void setupCache(final Supply supply) {
			final NamespaceId namespaceId = this.mosaicId.getNamespaceId();
			this.namespaceCache.add(new Namespace(namespaceId, this.sender, BlockHeight.ONE));
			final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(this.sender, this.mosaicId, Utils.createMosaicProperties());
			final MosaicEntry mosaicEntry = this.namespaceCache.get(namespaceId).getMosaics().add(mosaicDefinition);
			mosaicEntry.increaseSupply(supply);
		}

		private MosaicTransferNotification createMosaicTransferNotification(final Quantity quantity) {
			return new MosaicTransferNotification(
					this.sender,
					this.recipient,
					this.mosaicId,
					quantity);
		}

		private void decrementMosaicBalance(final Account account, final Quantity quantity) {
			final MosaicEntry mosaicEntry = this.namespaceCache.get(this.mosaicId.getNamespaceId()).getMosaics().get(this.mosaicId);
			mosaicEntry.getBalances().decrementBalance(account.getAddress(), quantity);
		}
	}
}
