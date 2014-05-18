package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.test.Utils;

public class HistoricalOutlinksTest {
	@Test
	public void canCreateHistoricalOutlinks() {
		// Arrange:
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Assert:
		Assert.assertThat(historicalOutlinks.size(), IsEqual.equalTo(0));
	}

	//region add
	@Test
	public void canAddOutlinkToHistoricalOutlink() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), account, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1234), account, Amount.fromNem(789));

		// Assert:
		Assert.assertThat(historicalOutlinks.size(), IsEqual.equalTo(1));
		Assert.assertThat(historicalOutlinks.getLastHistoricalOutlink().getHeight(), IsEqual.equalTo(new BlockHeight(1234)));
	}

	@Test
	public void canAddOutlinksToHistoricalOutlink() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), account, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1235), account, Amount.fromNem(789));

		// Assert:
		Assert.assertThat(historicalOutlinks.size(), IsEqual.equalTo(2));
		Assert.assertThat(historicalOutlinks.getLastHistoricalOutlink().getHeight(), IsEqual.equalTo(new BlockHeight(1235)));
	}
	//endregion

	//region remove
	@Test
	public void canRemoveOutlinkFromHistoricalOutlink() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), account, Amount.fromNem(789));
		historicalOutlinks.remove(new BlockHeight(1234), account, Amount.fromNem(789));

		// Assert:
		Assert.assertThat(historicalOutlinks.size(), IsEqual.equalTo(0));
	}

	@Test
	public void canRemoveOutlinkFromFewHistoricalOutlink() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), account, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1235), account, Amount.fromNem(789));
		historicalOutlinks.remove(new BlockHeight(1235), account, Amount.fromNem(789));

		// Assert:
		Assert.assertThat(historicalOutlinks.size(), IsEqual.equalTo(1));
		Assert.assertThat(historicalOutlinks.getLastHistoricalOutlink().getHeight(), IsEqual.equalTo(new BlockHeight(1234)));
	}


	@Test(expected = IllegalArgumentException.class)
	public void removingAmountNotInOrderThrowsException() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), account, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1234), account, Amount.fromNem(123));
		historicalOutlinks.remove(new BlockHeight(1234), account, Amount.fromNem(789));
	}

	@Test(expected = IllegalArgumentException.class)
	public void removingHeightNotInOrderThrowsException() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final HistoricalOutlinks historicalOutlinks = new HistoricalOutlinks();

		// Act:
		historicalOutlinks.add(new BlockHeight(1234), account, Amount.fromNem(789));
		historicalOutlinks.add(new BlockHeight(1235), account, Amount.fromNem(789));
		historicalOutlinks.remove(new BlockHeight(1234), account, Amount.fromNem(789));
	}
	//endregion
}
