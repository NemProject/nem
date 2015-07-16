package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.observers.TransferObserver;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;

/**
 * Transfer observer that commits balance changes to the underlying accounts.
 * TODO 20150715 J-B: we should update the tests for this
 */
public class BalanceCommitTransferObserver implements TransferObserver {
	private final AccountStateCache accountStateCache;

	/**
	 * Creates an observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public BalanceCommitTransferObserver(final AccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
		this.notifyDebit(sender, amount);
		this.notifyCredit(recipient, amount);
	}

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final SmartTile smartTile) {
		this.notifyDebit(sender,smartTile);
		this.notifyCredit(recipient, smartTile);
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
		this.getAccountInfo(account).incrementBalance(amount);
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
		this.getAccountInfo(account).decrementBalance(amount);
	}

	public void notifyCredit(final Account account, final SmartTile smartTile) {
		// TODO 20150715 J-B: so i guess i deleted multiply, you added it back, and i deleted it again ^^
		// > but i don't really understand what you're doing here; what's quantity and smartTile.quantity?
		// TODO 20150716 BR -> J: see trello mosaic card. I will change it so that notifyCredit will only have the smartTile parameter.
//		this.getSmartTileMap(account).add(new SmartTile(smartTile.getMosaicId(), smartTile.getQuantity().multiply(quantity)));
	}

	public void notifyDebit(final Account account, final SmartTile smartTile) {
//		this.getSmartTileMap(account).subtract(new SmartTile(smartTile.getMosaicId(), smartTile.getQuantity().multiply(quantity)));
	}

	private AccountInfo getAccountInfo(final Account account) {
		return this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo();
	}

	private SmartTileMap getSmartTileMap(final Account account) {
		return this.accountStateCache.findStateByAddress(account.getAddress()).getSmartTileMap();
	}
}
