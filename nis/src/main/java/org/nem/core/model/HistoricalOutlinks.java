package org.nem.core.model;

import sun.management.resources.agent_pt_BR;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 */
public class HistoricalOutlinks {
	private final LinkedList<HistoricalOutlink> outlinks = new LinkedList<>();
	
	/**
	 * Add an outlink at a given block height.
	 *
	 * @param height the height where the outlink is inserted
	 * @param otherAccount linked account
	 * @param amount link strength
	 */
	public void add(final BlockHeight height, final Account otherAccount, final Amount amount) {
		if (outlinks.size() == 0 || outlinks.getLast().getHeight().compareTo(height) < 0) {
			outlinks.addLast(new HistoricalOutlink(height));
		}

		outlinks.getLast().add(new AccountLink(height, amount, otherAccount));
	}

	/**
	 * Remove an outlink at a given block height.
	 *
	 * @param height the height where the outlink is to be removed
	 * @param otherAccount linked account
	 * @param amount link strength
	 */
	public void remove(final BlockHeight height, final Account otherAccount, final Amount amount) {
		if (outlinks.size() == 0 || outlinks.getLast().getHeight().compareTo(height) != 0) {
			throw new IllegalArgumentException("unexpected height, add/remove must be 'paired'.");
		}
		final AccountLink accountLink = new AccountLink(height, amount, otherAccount);
		outlinks.getLast().remove(accountLink);
		if (outlinks.getLast().size() == 0) {
			outlinks.removeLast();
		}
	}

	/**
	 * Returns number of AccountLink up to (inclusive) given height
	 *
	 * @param blockHeight the height.
	 * @return number of AccountLink
	 */
	public int outlinksSize(final BlockHeight blockHeight) {
		return outlinks.stream()
				.filter(x -> x.getHeight().compareTo(blockHeight) <= 0)
				.map(a -> a.size())
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
		return outlinks.stream()
				.map(a -> a.size())
				.reduce(0, Integer::sum);
	}

	public HistoricalOutlink getLastHistoricalOutlink() {
		return this.outlinks.getLast();
	}
}
