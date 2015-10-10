package org.nem.nis.state;

import org.nem.core.model.primitive.BlockHeight;

import java.util.LinkedList;

/**
 * Represents a historical outlink.
 */
public class HistoricalOutlink {

	private final BlockHeight height;
	private final LinkedList<AccountLink> outlinks;

	/**
	 * Creates a new historical outlink.
	 *
	 * @param height The block height.
	 */
	public HistoricalOutlink(final BlockHeight height) {
		super();
		this.height = height;
		this.outlinks = new LinkedList<>();
	}

	/**
	 * Returns height of current outlink.
	 *
	 * @return The height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Returns all outlinks.
	 *
	 * @return All outlinks.
	 */
	public LinkedList<AccountLink> getOutlinks() {
		return this.outlinks;
	}

	/**
	 * Returns the number of outlinks.
	 *
	 * @return The number of outlinks.
	 */
	public int size() {
		return this.outlinks.size();
	}

	/**
	 * Adds an outlink.
	 *
	 * @param accountLink The outlink to add.
	 */
	public void add(final AccountLink accountLink) {
		this.outlinks.addLast(accountLink);
	}

	/**
	 * Removes an outlink.
	 *
	 * @param accountLink The outlink to remove.
	 */
	public void remove(final AccountLink accountLink) {
		if (this.outlinks.getLast().compareTo(accountLink) != 0) {
			throw new IllegalArgumentException("add/remove must be 'paired'.");
		}

		this.outlinks.removeLast();
	}

	/**
	 * Creates a new copy of this outlink.
	 *
	 * @return A copy of this historical outlink.
	 */
	public HistoricalOutlink copy() {
		final HistoricalOutlink copy = new HistoricalOutlink(this.height);

		// since AccountLink is immutable, they don't need to be copied
		this.outlinks.forEach(copy::add);
		return copy;
	}
}
