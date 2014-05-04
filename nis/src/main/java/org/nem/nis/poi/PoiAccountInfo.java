package org.nem.nis.poi;

import org.nem.core.model.Account;
import org.nem.core.model.Amount;
import org.nem.core.model.BlockHeight;

import java.util.List;

/**
 * Account information used by poi.
 */
public class PoiAccountInfo {

	private static final Amount MIN_FORAGING_BALANCE = Amount.fromNem(1);

	private final int index;
	private final Account account;

	/**
	 * Creates a new POI account info.
	 *
	 * @param index The temporal account index.
	 * @param account The account.
	 */
	public PoiAccountInfo(final int index, final Account account) {
		this.index = index;
		this.account = account;
	}

	/**
	 * Gets the account index.
	 *
	 * @return The account index.
	 */
	public int getIndex() { return this.index; }

	/**
	 * Gets the account.
	 *
	 * @return The account.
	 */
	public Account getAccount() { return this.account; }

	/**
	 * Determines whether or not the account is eligible for foraging at the specified block height.
	 *
	 * @param height The block height.
	 * @return true if the account is eligible.
	 */
	public boolean canForage(final BlockHeight height) {
		return this.account.getCoinDayWeightedBalance(height).compareTo(MIN_FORAGING_BALANCE) >= 0
				&& this.account.getBalance().compareTo(MIN_FORAGING_BALANCE) >= 0;
	}

	/**
	 * Determines if the account has any out-links.
	 *
	 * @return true if the account has any out-links.
	 */
	public boolean hasOutLinks() {
		final List<?> outLinks = this.account.getOutlinks();
		return null != outLinks && !outLinks.isEmpty();
	}
}