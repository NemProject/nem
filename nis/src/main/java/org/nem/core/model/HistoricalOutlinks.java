package org.nem.core.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * A collection of historical out-links.
 */
public class HistoricalOutlinks {

	private final LinkedList<HistoricalOutlink> outlinks = new LinkedList<>();
	
	/**
	 * Add an outlink at a given block height.
	 *
	 * @param height the height where the outlink is inserted
	 * @param otherAccountAddress linked account address
	 * @param amount link strength
	 */
	public void add(final BlockHeight height, final Address otherAccountAddress, final Amount amount) {
		if (this.outlinks.size() == 0 || this.outlinks.getLast().getHeight().compareTo(height) < 0) {
			this.outlinks.addLast(new HistoricalOutlink(height));
		}

		this.outlinks.getLast().add(new AccountLink(height, amount, otherAccountAddress));
	}

	/**
	 * Remove an outlink at a given block height.
	 *
	 * @param height the height where the outlink is to be removed
	 * @param otherAccountAddress linked account address
	 * @param amount link strength
	 */
	public void remove(final BlockHeight height, final Address otherAccountAddress, final Amount amount) {
		if (this.outlinks.size() == 0 || this.outlinks.getLast().getHeight().compareTo(height) != 0) {
			throw new IllegalArgumentException("unexpected height, add/remove must be 'paired'.");
		}

		final AccountLink accountLink = new AccountLink(height, amount, otherAccountAddress);
		this.outlinks.getLast().remove(accountLink);
		if (this.outlinks.getLast().size() == 0) {
			this.outlinks.removeLast();
		}
	}

	/**
	 * Returns number of AccountLink up to (inclusive) given height
	 *
	 * @param blockHeight the height.
	 * @return number of AccountLink
	 */
	public int outlinksSize(final BlockHeight blockHeight) {
		return this.outlinks.stream()
				.filter(x -> x.getHeight().compareTo(blockHeight) <= 0)
				.map(hl -> hl.size())
				.reduce(0, Integer::sum);
	}

	/**
	 * Returns iterator over AccountLink up to (inclusive) given height
	 *
	 * @param blockHeight the height.
	 * @return iterator
	 */
	public Iterator<AccountLink> outlinksIterator(final BlockHeight blockHeight) {
		return this.outlinks.stream()
				.filter(x -> x.getHeight().compareTo(blockHeight) <= 0)
				.flatMap(x -> x.getOutlinks().stream())
				.iterator();
	}

	/**
	 * Returns number of ALL AccountLink in this container.
	 *
	 * @return number of AccountLink
	 */
	public int outlinkSize() {
		return this.outlinks.stream()
				.map(hl -> hl.size())
				.reduce(0, Integer::sum);
	}

	/**
	 * Gets the last historical out-link.
	 *
	 * @return The last historical out-link.
	 */
	public HistoricalOutlink getLastHistoricalOutlink() {
		return this.outlinks.getLast();
	}

	/**
	 * Creates a new copy of these out-links.
	 */
	public HistoricalOutlinks copy() {
		final HistoricalOutlinks copy = new HistoricalOutlinks();
		// looks ugly, but obfuscation does not like forEach :/
		for (final HistoricalOutlink temp : this.outlinks.stream().map(hl -> hl.copy()).collect(Collectors.toList()) )
		{
			copy.outlinks.add(temp);
		}
		return copy;
	}
}
