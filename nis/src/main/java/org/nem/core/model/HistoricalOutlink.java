package org.nem.core.model;

import java.util.LinkedList;

public class HistoricalOutlink {
	private final BlockHeight height;
	private LinkedList<AccountLink> outlinks;
	/**
	 * @param height
	 * @param outlinks
	 */
	public HistoricalOutlink(BlockHeight height) {
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
		return height;
	}

	public void add(final AccountLink accountLink) {
		this.outlinks.addLast(accountLink);
	}

	public void remove(final AccountLink accountLink) {
		if (this.outlinks.getLast().compareTo(accountLink) != 0) {
			throw new IllegalArgumentException("add/remove must be 'paired'.");
		}
		this.outlinks.removeLast();
	}

	public int size() {
		return this.outlinks.size();
	}
}
