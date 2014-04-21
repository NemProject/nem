package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class HistoricalBalancesTest {

	//region Retrieval
	@Test
	public void historicalBalanceCanBeRetrieved() {
		// Arrange:
		final HistoricalBalances balances = new HistoricalBalances();
		BlockHeight lastBlockHeight = new BlockHeight(40L);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L), lastBlockHeight);
		balances.add(new BlockHeight(20L), new Amount(2L), lastBlockHeight);
		balances.add(new BlockHeight(25L), new Amount(4L), lastBlockHeight);
		balances.add(new BlockHeight(30L), new Amount(8L), lastBlockHeight);

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(31L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(30L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(29L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(25L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(24L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(20L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(19L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(10L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(9L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
	}
	//endregion


	//region add/subtract
	@Test
	public void historicalBalanceCanBeAdded() {
		// Arrange:
		final HistoricalBalances balances = new HistoricalBalances();
		BlockHeight lastBlockHeight = new BlockHeight(40L);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L), lastBlockHeight);
		balances.add(new BlockHeight(20L), new Amount(2L), lastBlockHeight);
		balances.add(new BlockHeight(30L), new Amount(4L), lastBlockHeight);
		balances.add(new BlockHeight(10L), new Amount(100L), lastBlockHeight);
		balances.add(new BlockHeight(20L), new Amount(200L), lastBlockHeight);
		balances.add(new BlockHeight(30L), new Amount(400L), lastBlockHeight);

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(31L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(707L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(30L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(707L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(20L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(303L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(10L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(101L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(5L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
	
	}

	@Test
	public void historicalBalanceCanBeSubtracted() {
		// Arrange:
		final HistoricalBalances balances = new HistoricalBalances();
		BlockHeight lastBlockHeight = new BlockHeight(40L);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(101L), lastBlockHeight);
		balances.add(new BlockHeight(20L), new Amount(202L), lastBlockHeight);
		balances.add(new BlockHeight(30L), new Amount(404L), lastBlockHeight);
		balances.subtract(new BlockHeight(10L), new Amount(1L), lastBlockHeight);
		balances.subtract(new BlockHeight(20L), new Amount(2L), lastBlockHeight);
		balances.subtract(new BlockHeight(30L), new Amount(4L), lastBlockHeight);

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(31L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(30L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(20L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(300L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(10L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(100L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(5L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
	
	}

	@Test
	public void historicalBalanceCanBeSubtracted2() {
		// Arrange:
		final HistoricalBalances balances = new HistoricalBalances();
		BlockHeight lastBlockHeight = new BlockHeight(40L);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(101L), lastBlockHeight);
		balances.add(new BlockHeight(20L), new Amount(202L), lastBlockHeight);
		balances.add(new BlockHeight(30L), new Amount(404L), lastBlockHeight);
		balances.subtract(new BlockHeight(15L), new Amount(1L), lastBlockHeight);
		balances.subtract(new BlockHeight(25L), new Amount(2L), lastBlockHeight);
		balances.subtract(new BlockHeight(35L), new Amount(4L), lastBlockHeight);

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(35L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(34L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(704L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(29L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(300L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(24L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(302L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(19L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(100L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(14L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(101L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(9L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
	
	}
	//endregion

	//region trim
	@Test
	public void historicalBalanceCanBeTrimmed() {
		// Arrange:
		final HistoricalBalances balances = new HistoricalBalances();
		BlockHeight lastBlockHeight = new BlockHeight(1000L);
		
		// Act:
		balances.add(new BlockHeight(100L), new Amount(1L), lastBlockHeight);
		balances.add(new BlockHeight(200L), new Amount(2L), lastBlockHeight);
		balances.add(new BlockHeight(300L), new Amount(4L), lastBlockHeight);

		// Assert:
		Assert.assertThat(balances.size(), IsEqual.equalTo(3));

		// Act:
		lastBlockHeight = new BlockHeight(3000L);
		balances.add(new BlockHeight(2500L), new Amount(8L), lastBlockHeight);

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(2500L), lastBlockHeight).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.size(), IsEqual.equalTo(1));
	}
	//endregion
}
