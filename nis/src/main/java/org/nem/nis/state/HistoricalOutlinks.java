package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A collection of historical outlinks.
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
		if (this.outlinks.isEmpty() || this.outlinks.getLast().getHeight().compareTo(height) < 0) {
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
		if (this.outlinks.isEmpty() || this.outlinks.getLast().getHeight().compareTo(height) != 0) {
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
		return this.outlinks.stream().filter(x -> x.getHeight().compareTo(blockHeight) <= 0).map(HistoricalOutlink::size).reduce(0,
				Integer::sum);
	}

	/**
	 * Returns iterator over AccountLink between (inclusive) given start and end height.
	 *
	 * @param startHeight the start height.
	 * @param endHeight the end height.
	 * @return iterator
	 */
	public Iterator<AccountLink> outlinksIterator(final BlockHeight startHeight, final BlockHeight endHeight) {
		return this.outlinks.stream().filter(x -> x.getHeight().compareTo(startHeight) >= 0 && x.getHeight().compareTo(endHeight) <= 0)
				.flatMap(x -> x.getOutlinks().stream()).iterator();
	}

	/**
	 * Returns number of ALL AccountLink in this container.
	 *
	 * @return number of AccountLink
	 */
	public int outlinkSize() {
		return this.outlinks.stream().map(HistoricalOutlink::size).reduce(0, Integer::sum);
	}

	/**
	 * Gets the last historical outlink.
	 *
	 * @return The last historical outlink.
	 */
	public HistoricalOutlink getLastHistoricalOutlink() {
		return this.outlinks.getLast();
	}

	/**
	 * Removes all historical outlinks that are older than the specified height.
	 *
	 * @param minHeight The minimum height of outlinks to keep.
	 */
	public void prune(final BlockHeight minHeight) {
		this.outlinks.removeIf(outlink -> outlink.getHeight().compareTo(minHeight) < 0);
	}

	/**
	 * Creates a new copy of these outlinks.
	 *
	 * @return A copy of these historical outlinks.
	 */
	public HistoricalOutlinks copy() {
		final HistoricalOutlinks copy = new HistoricalOutlinks();
		copy.outlinks.addAll(this.outlinks.stream().map(HistoricalOutlink::copy).collect(Collectors.toList()));
		return copy;
	}
}
