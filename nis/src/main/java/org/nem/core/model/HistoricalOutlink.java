package org.nem.core.model;

import java.util.LinkedList;

/**
 * Represents a historical out-link
 */
public class HistoricalOutlink {

	private final BlockHeight height;
	private final LinkedList<AccountLink> outlinks;

	/**
	 * Creates a new historical out-link.
	 *
	 * @param height The block height.
	 */
	public HistoricalOutlink(final BlockHeight height) {
		super();
		this.height = height;
		this.outlinks = new LinkedList<>();
	}

	/**
	 * Returns height of current out-link.
	 *
	 * @return The height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Returns all out-links.
	 *
	 * @return All out-links.
	 */
	public LinkedList<AccountLink> getOutlinks() {
		return this.outlinks;
	}

	/**
	 * Returns the number of out-links.
	 *
	 * @return The number of out-links.
	 */
	public int size() {
		return this.outlinks.size();
	}

	/**
	 * Adds an out-link.
	 *
	 * @param accountLink The out-link to add.
	 */
	public void add(final AccountLink accountLink) {
		this.outlinks.addLast(accountLink);
	}

	/**
	 * Removes an out-link.
	 *
	 * @param accountLink The out-link to remove.
	 */
	public void remove(final AccountLink accountLink) {
		if (this.outlinks.getLast().compareTo(accountLink) != 0) {
			throw new IllegalArgumentException("add/remove must be 'paired'.");
		}

		this.outlinks.removeLast();
	}

	/**
	 * Creates a new copy of this out-link.
	 */
	public HistoricalOutlink copy() {
		final HistoricalOutlink copy = new HistoricalOutlink(this.height);

		// since AccountLink is immutable, they don't need to be copied
		for (final AccountLink accountLink : this.outlinks) {
			copy.add(accountLink);
		}
		return copy;
	}
}
