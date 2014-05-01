package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.nis.service.BlockChainLastBlockLayer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HistoricalBalancesTest {

	//region Retrieval
	@Test
	public void historicalBalanceCanBeRetrieved() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances(40L);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L));
		balances.add(new BlockHeight(20L), new Amount(2L));
		balances.add(new BlockHeight(25L), new Amount(4L));
		balances.add(new BlockHeight(30L), new Amount(8L));

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(31L)).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(30L)).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(29L)).getBalance().getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(25L)).getBalance().getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(24L)).getBalance().getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(20L)).getBalance().getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(19L)).getBalance().getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(10L)).getBalance().getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(9L)).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
	}
	//endregion


	//region add/subtract
	@Test
	public void historicalBalanceCanBeAdded() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances(40L);

		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L));
		balances.add(new BlockHeight(20L), new Amount(2L));
		balances.add(new BlockHeight(30L), new Amount(4L));
		balances.add(new BlockHeight(10L), new Amount(100L));
		balances.add(new BlockHeight(20L), new Amount(200L));
		balances.add(new BlockHeight(30L), new Amount(400L));

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(31L)).getBalance().getNumMicroNem(), IsEqual.equalTo(707L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(30L)).getBalance().getNumMicroNem(), IsEqual.equalTo(707L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(20L)).getBalance().getNumMicroNem(), IsEqual.equalTo(303L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(10L)).getBalance().getNumMicroNem(), IsEqual.equalTo(101L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(5L)).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
	
	}

	@Test
	public void historicalBalanceCanBeSubtracted() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances(40L);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(101L));
		balances.add(new BlockHeight(20L), new Amount(202L));
		balances.add(new BlockHeight(30L), new Amount(404L));
		balances.subtract(new BlockHeight(10L), new Amount(1L));
		balances.subtract(new BlockHeight(20L), new Amount(2L));
		balances.subtract(new BlockHeight(30L), new Amount(4L));

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(31L)).getBalance().getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(30L)).getBalance().getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(20L)).getBalance().getNumMicroNem(), IsEqual.equalTo(300L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(10L)).getBalance().getNumMicroNem(), IsEqual.equalTo(100L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(5L)).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
	
	}

	@Test
	public void historicalBalanceCanBeSubtracted2() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances(40L);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(101L));
		balances.add(new BlockHeight(20L), new Amount(202L));
		balances.add(new BlockHeight(30L), new Amount(404L));
		balances.subtract(new BlockHeight(15L), new Amount(1L));
		balances.subtract(new BlockHeight(25L), new Amount(2L));
		balances.subtract(new BlockHeight(35L), new Amount(4L));

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(35L)).getBalance().getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(34L)).getBalance().getNumMicroNem(), IsEqual.equalTo(704L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(29L)).getBalance().getNumMicroNem(), IsEqual.equalTo(300L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(24L)).getBalance().getNumMicroNem(), IsEqual.equalTo(302L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(19L)).getBalance().getNumMicroNem(), IsEqual.equalTo(100L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(14L)).getBalance().getNumMicroNem(), IsEqual.equalTo(101L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(9L)).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
	
	}
	//endregion

	//region trim
	@Test
	public void historicalBalanceCanBeTrimmed() {
		// Arrange:
		final BlockChainLastBlockLayer blockChainLastBlockLayer = mock(BlockChainLastBlockLayer.class);
		when(blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(1000L);
		final HistoricalBalances balances = new HistoricalBalances(blockChainLastBlockLayer);
		
		// Act:
		balances.add(new BlockHeight(100L), new Amount(1L));
		balances.add(new BlockHeight(200L), new Amount(2L));
		balances.add(new BlockHeight(300L), new Amount(4L));

		// Assert:
		Assert.assertThat(balances.size(), IsEqual.equalTo(3));

		// Act:
		when(blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(3000L);
		balances.add(new BlockHeight(2500L), new Amount(8L));

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(2500L)).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getHistoricalBalance(new BlockHeight(2000L)).getBalance().getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.size(), IsEqual.equalTo(2));
	}
	//endregion

	private static HistoricalBalances createTestHistoricalBalances(long l) {
		final BlockChainLastBlockLayer blockChainLastBlockLayer = mock(BlockChainLastBlockLayer.class);
		when(blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(40L);
		return new HistoricalBalances(blockChainLastBlockLayer);
	}
}
