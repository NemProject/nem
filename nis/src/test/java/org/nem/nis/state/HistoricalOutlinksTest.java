package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

import java.util.*;
import java.util.stream.*;

import static org.hamcrest.core.IsEqual.equalTo;

public class HistoricalOutlinksTest {
	@Test
	public void canCreateHistoricalOutlinks() {
		// Arrange:
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Assert:
		MatcherAssert.assertThat(historicalOutlinks.outlinkSize(), equalTo(0));
	}

	// region add

	@Test
	public void canAddOutlinkToHistoricalOutlink() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(789));

		// Assert:
		MatcherAssert.assertThat(historicalOutlinks.outlinkSize(), equalTo(2));
		MatcherAssert.assertThat(historicalOutlinks.getLastHistoricalOutlink().getHeight(), equalTo(new BlockHeight(1234)));
	}

	@Test
	public void canAddOutlinksToHistoricalOutlink() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(789));

		// Assert:
		MatcherAssert.assertThat(historicalOutlinks.outlinkSize(), equalTo(2));
		MatcherAssert.assertThat(historicalOutlinks.getLastHistoricalOutlink().getHeight(), equalTo(new BlockHeight(1235)));
	}

	// endregion

	// region remove

	@Test
	public void canRemoveOutlinkFromHistoricalOutlink() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(789));
		historicalOutlinks.remove(new BlockHeight(1234), address, Amount.fromNem(789));

		// Assert:
		MatcherAssert.assertThat(historicalOutlinks.outlinkSize(), equalTo(0));
	}

	@Test
	public void canRemoveOutlinkFromFewHistoricalOutlink() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(789));
		historicalOutlinks.remove(new BlockHeight(1235), address, Amount.fromNem(789));

		// Assert:
		MatcherAssert.assertThat(historicalOutlinks.outlinkSize(), equalTo(1));
		MatcherAssert.assertThat(historicalOutlinks.getLastHistoricalOutlink().getHeight(), equalTo(new BlockHeight(1234)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void removingAmountNotInOrderThrowsException() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(123));
		historicalOutlinks.remove(new BlockHeight(1234), address, Amount.fromNem(789));
	}

	@Test(expected = IllegalArgumentException.class)
	public void removingHeightNotInOrderThrowsException() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(789));
		historicalOutlinks.remove(new BlockHeight(1234), address, Amount.fromNem(789));
	}

	// endregion

	// region size/iterator

	@Test
	public void historicalOutlinksSizeReturnsProperValue() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = createDefaultHistoricalOutlinks(address);

		// Assert:
		MatcherAssert.assertThat(historicalOutlinks.outlinksSize(new BlockHeight(1225)), equalTo(0));
		MatcherAssert.assertThat(historicalOutlinks.outlinksSize(new BlockHeight(1235)), equalTo(4));
		MatcherAssert.assertThat(historicalOutlinks.outlinksSize(new BlockHeight(1236)), equalTo(5));
		MatcherAssert.assertThat(historicalOutlinks.outlinksSize(new BlockHeight(1275)), equalTo(6));
	}

	@Test
	public void historicalOutlinksIteratorReturnsProperValuesWhenTruncatedByEndHeight() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = createDefaultHistoricalOutlinks(address);

		// Act:
		final Iterator<AccountLink> it = historicalOutlinks.outlinksIterator(new BlockHeight(1225), new BlockHeight(1235));

		// Assert:
		assertLinkAmounts(it, 123, 234, 345, 456);
	}

	@Test
	public void historicalOutlinksIteratorReturnsProperValuesWhenTruncatedByStartHeight() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = createDefaultHistoricalOutlinks(address);

		// Act:
		final Iterator<AccountLink> it = historicalOutlinks.outlinksIterator(new BlockHeight(1235), new BlockHeight(1275));

		// Assert:
		assertLinkAmounts(it, 345, 456, 567, 678);
	}

	@Test
	public void historicalOutlinksIteratorReturnsProperValuesWhenTruncatedByStartAndEndHeight() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = createDefaultHistoricalOutlinks(address);

		// Act:
		final Iterator<AccountLink> it = historicalOutlinks.outlinksIterator(new BlockHeight(1235), new BlockHeight(1236));

		// Assert:
		assertLinkAmounts(it, 345, 456, 567);
	}

	private static HistoricalOutlinks createDefaultHistoricalOutlinks(final Address address) {
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(123));
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(234));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(345));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(456));
		historicalOutlinks.add(new BlockHeight(1236), address, Amount.fromNem(567));
		historicalOutlinks.add(new BlockHeight(1237), address, Amount.fromNem(678));
		return historicalOutlinks;
	}

	private static void assertLinkAmounts(final Iterator<AccountLink> it, final int... expectedAmounts) {
		for (final int expectedAmount : expectedAmounts) {
			MatcherAssert.assertThat(it.next().getAmount(), equalTo(Amount.fromNem(expectedAmount)));
		}

		Assert.assertFalse(it.hasNext());
	}

	// endregion

	// region prune

	@Test
	public void pruneRemovesAllOlderOutlinks() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(123));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(234));
		historicalOutlinks.add(new BlockHeight(1236), address, Amount.fromNem(345));
		historicalOutlinks.add(new BlockHeight(1237), address, Amount.fromNem(456));

		// Act:
		historicalOutlinks.prune(new BlockHeight(1236));

		// Assert:
		final Iterator<AccountLink> it = historicalOutlinks.outlinksIterator(BlockHeight.ONE, new BlockHeight(1238));
		final List<Long> amounts = StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.IMMUTABLE), false)
				.map(link -> link.getAmount().getNumNem()).collect(Collectors.toList());
		MatcherAssert.assertThat(amounts, IsEqual.equalTo(Arrays.asList(345L, 456L)));
	}

	// endregion

	// region copy

	@Test
	public void copyCopiesOutlinks() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(123));
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(234));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(345));

		// Act:
		final HistoricalOutlinks copy = historicalOutlinks.copy();

		// Assert:
		final Iterator<AccountLink> it = copy.outlinksIterator(BlockHeight.ONE, new BlockHeight(1238));
		final List<Long> amounts = StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.IMMUTABLE), false)
				.map(link -> link.getAmount().getNumNem()).collect(Collectors.toList());
		MatcherAssert.assertThat(amounts, IsEqual.equalTo(Arrays.asList(123L, 234L, 345L)));
	}

	@Test
	public void copyCreatesDeepCopyOfOutlinks() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(123));
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(234));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(345));

		// Act:
		final HistoricalOutlinks copy = historicalOutlinks.copy();
		copy.add(new BlockHeight(1235), address, Amount.fromNem(111));

		// Assert:
		MatcherAssert.assertThat(historicalOutlinks.getLastHistoricalOutlink().size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(copy.getLastHistoricalOutlink().size(), IsEqual.equalTo(2));
	}

	// endregion
}
