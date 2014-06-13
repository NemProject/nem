package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;

import java.util.Iterator;

import static org.hamcrest.core.IsEqual.equalTo;

public class HistoricalOutlinksTest {
	@Test
	public void canCreateHistoricalOutlinks() {
		// Arrange:
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Assert:
		Assert.assertThat(historicalOutlinks.outlinkSize(), equalTo(0));
	}

	//region add
	@Test
	public void canAddOutlinkToHistoricalOutlink() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(789));

		// Assert:
		Assert.assertThat(historicalOutlinks.outlinkSize(), equalTo(2));
		Assert.assertThat(historicalOutlinks.getLastHistoricalOutlink().getHeight(), equalTo(new BlockHeight(1234)));
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
		Assert.assertThat(historicalOutlinks.outlinkSize(), equalTo(2));
		Assert.assertThat(historicalOutlinks.getLastHistoricalOutlink().getHeight(), equalTo(new BlockHeight(1235)));
	}
	//endregion

	//region remove
	@Test
	public void canRemoveOutlinkFromHistoricalOutlink() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(789));
		historicalOutlinks.remove(new BlockHeight(1234), address, Amount.fromNem(789));

		// Assert:
		Assert.assertThat(historicalOutlinks.outlinkSize(), equalTo(0));
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
		Assert.assertThat(historicalOutlinks.outlinkSize(), equalTo(1));
		Assert.assertThat(historicalOutlinks.getLastHistoricalOutlink().getHeight(), equalTo(new BlockHeight(1234)));
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
	//endregion

	//region size/iterator
	@Test
	public void historicalOutlinksSizeReturnsProperValue() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(123));
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(234));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(345));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(456));
		historicalOutlinks.add(new BlockHeight(1236), address, Amount.fromNem(567));

		// Assert:
		Assert.assertThat(historicalOutlinks.outlinksSize(new BlockHeight(1235)), equalTo(4));
	}

	@Test
	public void historicalOutlinksIteratorReturnsProperValues() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(123));
		historicalOutlinks.add(new BlockHeight(1234), address, Amount.fromNem(234));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(345));
		historicalOutlinks.add(new BlockHeight(1235), address, Amount.fromNem(456));
		historicalOutlinks.add(new BlockHeight(1236), address, Amount.fromNem(567));

		// Assert:
		final Iterator<AccountLink> it = historicalOutlinks.outlinksIterator(new BlockHeight(1235));
		Assert.assertThat(it.next().getAmount(), equalTo(Amount.fromNem(123)));
		Assert.assertThat(it.next().getAmount(), equalTo(Amount.fromNem(234)));
		Assert.assertThat(it.next().getAmount(), equalTo(Amount.fromNem(345)));
		Assert.assertThat(it.next().getAmount(), equalTo(Amount.fromNem(456)));
		Assert.assertFalse(it.hasNext());
	}
	//endregion

	//region copy

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
		final Iterator<AccountLink> it = copy.outlinksIterator(new BlockHeight(1235));
		Assert.assertThat(it.next().getAmount(), equalTo(Amount.fromNem(123)));
		Assert.assertThat(it.next().getAmount(), equalTo(Amount.fromNem(234)));
		Assert.assertThat(it.next().getAmount(), equalTo(Amount.fromNem(345)));
		Assert.assertFalse(it.hasNext());
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
		Assert.assertThat(historicalOutlinks.getLastHistoricalOutlink().size(), IsEqual.equalTo(1));
		Assert.assertThat(copy.getLastHistoricalOutlink().size(), IsEqual.equalTo(2));
	}

	//endregion
}
