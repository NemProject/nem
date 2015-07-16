package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;

public class BalanceCommitTransferObserverTest {

	@Test
	public void notifyTransferUpdatesAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();
		final ReadOnlyAccountInfo senderAccountInfo = context.add(sender, Amount.fromNem(100));
		final Account recipient = Utils.generateRandomAccount();
		final ReadOnlyAccountInfo recipientAccountInfo = context.add(recipient, Amount.fromNem(100));

		// Act:
		context.observer.notifyTransfer(sender, recipient, Amount.fromNem(20));

		// Assert:
		Assert.assertThat(senderAccountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(80)));
		Assert.assertThat(recipientAccountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(120)));
	}

	@Test
	public void notifyTransferUpdatesSmartTileMapForSmartTiles() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();
		final SmartTile smartTile = Utils.createSmartTile(123);
		final SmartTileMap senderSmartTileMap = context.add(sender, smartTile);
		final Account recipient = Utils.generateRandomAccount();
		final SmartTileMap recipientSmartTileMap = context.add(recipient, (SmartTile)null);

		// Act:
		context.observer.notifyTransfer(sender, recipient, smartTile);

		// Assert:
		Assert.assertThat(senderSmartTileMap.get(smartTile.getMosaicId()).getQuantity(), IsEqual.equalTo(Quantity.ZERO));
		Assert.assertThat(recipientSmartTileMap.get(smartTile.getMosaicId()).getQuantity(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	@Test
	public void notifyCreditUpdatesAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final ReadOnlyAccountInfo accountInfo = context.add(account, Amount.fromNem(100));

		// Act:
		context.observer.notifyCredit(account, Amount.fromNem(20));

		// Assert:
		Assert.assertThat(accountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(120)));
	}

	@Test
	public void notifyDebitUpdatesAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final ReadOnlyAccountInfo accountInfo = context.add(account, Amount.fromNem(100));

		// Act:
		context.observer.notifyDebit(account, Amount.fromNem(20));

		// Assert:
		Assert.assertThat(accountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(80)));
	}

	private static class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final BalanceCommitTransferObserver observer = new BalanceCommitTransferObserver(this.accountStateCache);

		public TestContext() {

		}
		public ReadOnlyAccountInfo add(final Account account, final Amount amount) {
			final AccountInfo accountInfo = new AccountInfo();
			accountInfo.incrementBalance(amount);

			final AccountState accountState = Mockito.mock(AccountState.class);
			Mockito.when(accountState.getAccountInfo()).thenReturn(accountInfo);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			return accountInfo;
		}

		public SmartTileMap add(final Account account, final SmartTile smartTile) {
			final SmartTileMap smartTileMap = new SmartTileMap();
			if (null != smartTile) {
				smartTileMap.add(smartTile);
			}

			final AccountState accountState = Mockito.mock(AccountState.class);
			Mockito.when(accountState.getSmartTileMap()).thenReturn(smartTileMap);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			return smartTileMap;
		}
	}
}