package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class HistoricalBalancesTest {

	//region add/subtract/retrieve
	@Test
	public void historicalBalanceCanBeaddedAndRetrieved() {
		// Arrange:
		final HistoricalBalances balances = new HistoricalBalances();
		
		// Act:
		balances.add(new BlockHeight(20L), new Amount(2L));
		balances.add(new BlockHeight(30L), new Amount(8L));
		balances.add(new BlockHeight(25L), new Amount(4L));
		balances.add(new BlockHeight(10L), new Amount(1L));

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(1L)).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(10L)).getBalance().getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(11L)).getBalance().getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(20L)).getBalance().getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(25L)).getBalance().getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(30L)).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(35L)).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
	}
	//endregion


	//region Retrieval
	@Test
	public void historicalBalanceCanBeRetrieved() {
		// Arrange:
		
		// Act:

		// Assert:
	
	}
	//endregion
}
