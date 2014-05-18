package org.nem.core.model;

import java.util.Iterator;

/**
 * Encapsulates management of an account's importance.
 */
public class AccountImportance {

	private final HistoricalOutlinks historicalOutLinks = new HistoricalOutlinks();

	private BlockHeight importanceHeight;
	private double importance;

	/**
	 * Adds an out-link.
	 *
	 * @param accountLink The account link to add.
	 */
	public void addOutLink(final AccountLink accountLink) {
		this.historicalOutLinks.add(accountLink.getHeight(), accountLink.getOtherAccount(), accountLink.getAmount());
	}

	/**
	 * Removes an out-link.
	 *
	 * @param accountLink The account link to remove.
	 */
	public void removeOutLink(final AccountLink accountLink) {
		this.historicalOutLinks.remove(accountLink.getHeight(), accountLink.getOtherAccount(), accountLink.getAmount());
	}

	/**
	 * Gets an iterator that returns all out-links at or before the (inclusive) given height.
	 *
	 * @param blockHeight The block height.
	 * @return The matching links.
	 */
	public Iterator<AccountLink> getOutLinksIterator(final BlockHeight blockHeight) {
		return this.historicalOutLinks.outlinksIterator(blockHeight);
	}

	/**
	 * Gets the number of out-links at or before the (inclusive) given height.
	 *
	 * @param blockHeight The block height.
	 * @return The number of matching links.
	 */
	public int getOutLinksSize(final BlockHeight blockHeight) {
		return this.historicalOutLinks.outlinksSize(blockHeight);
	}

	/**
	 * Sets the importance at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @param importance The importance.
	 */
	public void setImportance(final BlockHeight blockHeight, double importance) {
		if (null == importanceHeight || 0 == this.importanceHeight.compareTo(blockHeight)) {
			this.importanceHeight = blockHeight;
			this.importance = importance;

		} else if (this.importanceHeight.compareTo(blockHeight) != 0) {
			throw new IllegalArgumentException("importance already set at given height");
		}
	}

	/**
	 * Gets the importance at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @return The importance.
	 */
	public double getImportance(final BlockHeight blockHeight) {
		if (this.importanceHeight == null || 0 != this.importanceHeight.compareTo(blockHeight))
			throw new IllegalArgumentException("importance not set at wanted height");

		return importance;
	}
}
