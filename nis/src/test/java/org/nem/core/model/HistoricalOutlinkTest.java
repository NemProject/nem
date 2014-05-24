package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.test.Utils;

import java.util.NoSuchElementException;

public class HistoricalOutlinkTest {
	@Test
	public void historicalOutlinkCanBeConstructed() {
		// Arrange:
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(new BlockHeight(1234));

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(new BlockHeight(1234)));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(0));
	}

	//region add
	@Test
	public void canAddOutlinkToHistoricalOutlink() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);

		// Act:
		historicalOutlink.add(new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress()));

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(blockHeight));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(1));
	}

	@Test
	public void canAddOutlinksToHistoricalOutlink() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);

		// Act:
		historicalOutlink.add(new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress()));
		historicalOutlink.add(new AccountLink(blockHeight, Amount.fromNem(456), Utils.generateRandomAddress()));

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(blockHeight));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(2));
	}
	//endregion

	//region remove
	@Test
	public void canRemoveAddedOutlinksToHistoricalOutlink() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);
		final AccountLink accountLink1 = new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress());
		final AccountLink accountLink2 = new AccountLink(blockHeight, Amount.fromNem(456), Utils.generateRandomAddress());

		// Act:
		historicalOutlink.add(accountLink1);
		historicalOutlink.add(accountLink2);
		historicalOutlink.remove(accountLink2);
		historicalOutlink.remove(accountLink1);

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(blockHeight));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void removingOutlinksInWrongOrderThrowsException() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);
		final AccountLink accountLink1 = new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress());
		final AccountLink accountLink2 = new AccountLink(blockHeight, Amount.fromNem(456), Utils.generateRandomAddress());

		// Act:
		historicalOutlink.add(accountLink1);
		historicalOutlink.add(accountLink2);
		historicalOutlink.remove(accountLink1);
		historicalOutlink.remove(accountLink2);

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(blockHeight));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(0));
	}

	@Test(expected = NoSuchElementException.class)
	public void removingFromEmptyThrowsException() {
		// Arrange:
		final BlockHeight blockHeight = new BlockHeight(1234);
		final HistoricalOutlink historicalOutlink = new HistoricalOutlink(blockHeight);
		final AccountLink accountLink1 = new AccountLink(blockHeight, Amount.fromNem(789), Utils.generateRandomAddress());

		// Act:
		historicalOutlink.remove(accountLink1);

		// Assert:
		Assert.assertThat(historicalOutlink.getHeight(), IsEqual.equalTo(blockHeight));
		Assert.assertThat(historicalOutlink.size(), IsEqual.equalTo(0));
	}
	//endregion
}
