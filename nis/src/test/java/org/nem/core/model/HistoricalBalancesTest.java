package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
<<<<<<< HEAD
import org.nem.core.messages.PlainMessage;
import org.nem.core.test.Utils;
import org.nem.nis.BlockChain;
import org.nem.nis.test.MockBlockChain;
=======
import org.nem.nis.service.BlockChainLastBlockLayer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
>>>>>>> integration/20140501

public class HistoricalBalancesTest {

	//region Copy
	@Test
	public void historicalBalanceCanBeCopied() {
		// Arrange:
		BlockChain blockChain = new MockBlockChain();
		blockChain.getLastDbBlock().setHeight(40L);
		final HistoricalBalances balances = new HistoricalBalances();
		balances.setblockChain(blockChain);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L));
		balances.add(new BlockHeight(20L), new Amount(2L));
		balances.add(new BlockHeight(25L), new Amount(4L));
		balances.add(new BlockHeight(30L), new Amount(8L));
		HistoricalBalances balances2 = balances.copy();

		// Assert:
		Assert.assertThat(balances2.getBalance(new BlockHeight(31L)).getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances2.getBalance(new BlockHeight(30L)).getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances2.getBalance(new BlockHeight(29L)).getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances2.getBalance(new BlockHeight(25L)).getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances2.getBalance(new BlockHeight(24L)).getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances2.getBalance(new BlockHeight(20L)).getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances2.getBalance(new BlockHeight(19L)).getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances2.getBalance(new BlockHeight(10L)).getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances2.getBalance(new BlockHeight(9L)).getNumMicroNem(), IsEqual.equalTo(0L));
	}
	//rendregion
	
	//region Retrieval
	@Test
	public void historicalBalanceCanBeRetrieved() {
		// Arrange:
<<<<<<< HEAD
		BlockChain blockChain = new MockBlockChain();
		blockChain.getLastDbBlock().setHeight(40L);
		final HistoricalBalances balances = new HistoricalBalances();
		balances.setblockChain(blockChain);
=======
		final HistoricalBalances balances = createTestHistoricalBalances(40L);
>>>>>>> integration/20140501
		
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

	@Test
	public void balanceCanBeRetrieved() {
		// Arrange:
		BlockChain blockChain = new MockBlockChain();
		blockChain.getLastDbBlock().setHeight(40L);
		final HistoricalBalances balances = new HistoricalBalances();
		balances.setblockChain(blockChain);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L));
		balances.add(new BlockHeight(20L), new Amount(2L));
		balances.add(new BlockHeight(25L), new Amount(4L));
		balances.add(new BlockHeight(30L), new Amount(8L));

		// Assert:
		Assert.assertThat(balances.getBalance(new BlockHeight(31L)).getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getBalance(new BlockHeight(30L)).getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getBalance(new BlockHeight(29L)).getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getBalance(new BlockHeight(25L)).getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getBalance(new BlockHeight(24L)).getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getBalance(new BlockHeight(20L)).getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getBalance(new BlockHeight(19L)).getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getBalance(new BlockHeight(10L)).getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getBalance(new BlockHeight(9L)).getNumMicroNem(), IsEqual.equalTo(0L));
	}
	//endregion


	//region add/subtract
	@Test
	public void historicalBalanceCanBeAdded() {
		// Arrange:
<<<<<<< HEAD
		BlockChain blockChain = new MockBlockChain();
		blockChain.getLastDbBlock().setHeight(40L);
		final HistoricalBalances balances = new HistoricalBalances();
		balances.setblockChain(blockChain);
		
=======
		final HistoricalBalances balances = createTestHistoricalBalances(40L);

>>>>>>> integration/20140501
		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L));
		balances.add(new BlockHeight(20L), new Amount(2L));
		balances.add(new BlockHeight(30L), new Amount(4L));
		balances.add(new BlockHeight(10L), new Amount(100L));
		balances.add(new BlockHeight(20L), new Amount(200L));
		balances.add(new BlockHeight(30L), new Amount(400L));

		// Assert:
		Assert.assertThat(balances.getBalance(new BlockHeight(31L)).getNumMicroNem(), IsEqual.equalTo(707L));
		Assert.assertThat(balances.getBalance(new BlockHeight(30L)).getNumMicroNem(), IsEqual.equalTo(707L));
		Assert.assertThat(balances.getBalance(new BlockHeight(20L)).getNumMicroNem(), IsEqual.equalTo(303L));
		Assert.assertThat(balances.getBalance(new BlockHeight(10L)).getNumMicroNem(), IsEqual.equalTo(101L));
		Assert.assertThat(balances.getBalance(new BlockHeight(5L)).getNumMicroNem(), IsEqual.equalTo(0L));
	
	}

	@Test
	public void historicalBalanceCanBeSubtracted() {
		// Arrange:
<<<<<<< HEAD
		BlockChain blockChain = new MockBlockChain();
		blockChain.getLastDbBlock().setHeight(40L);
		final HistoricalBalances balances = new HistoricalBalances();
		balances.setblockChain(blockChain);
=======
		final HistoricalBalances balances = createTestHistoricalBalances(40L);
>>>>>>> integration/20140501
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(101L));
		balances.add(new BlockHeight(20L), new Amount(202L));
		balances.add(new BlockHeight(30L), new Amount(404L));
		balances.subtract(new BlockHeight(10L), new Amount(1L));
		balances.subtract(new BlockHeight(20L), new Amount(2L));
		balances.subtract(new BlockHeight(30L), new Amount(4L));

		// Assert:
		Assert.assertThat(balances.getBalance(new BlockHeight(31L)).getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getBalance(new BlockHeight(30L)).getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getBalance(new BlockHeight(20L)).getNumMicroNem(), IsEqual.equalTo(300L));
		Assert.assertThat(balances.getBalance(new BlockHeight(10L)).getNumMicroNem(), IsEqual.equalTo(100L));
		Assert.assertThat(balances.getBalance(new BlockHeight(5L)).getNumMicroNem(), IsEqual.equalTo(0L));
	
	}

	@Test
	public void historicalBalanceCanBeSubtracted2() {
		// Arrange:
<<<<<<< HEAD
		BlockChain blockChain = new MockBlockChain();
		blockChain.getLastDbBlock().setHeight(40L);
		final HistoricalBalances balances = new HistoricalBalances();
		balances.setblockChain(blockChain);
=======
		final HistoricalBalances balances = createTestHistoricalBalances(40L);
>>>>>>> integration/20140501
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(101L));
		balances.add(new BlockHeight(20L), new Amount(202L));
		balances.add(new BlockHeight(30L), new Amount(404L));
		balances.subtract(new BlockHeight(15L), new Amount(1L));
		balances.subtract(new BlockHeight(25L), new Amount(2L));
		balances.subtract(new BlockHeight(35L), new Amount(4L));

		// Assert:
		Assert.assertThat(balances.getBalance(new BlockHeight(35L)).getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getBalance(new BlockHeight(34L)).getNumMicroNem(), IsEqual.equalTo(704L));
		Assert.assertThat(balances.getBalance(new BlockHeight(29L)).getNumMicroNem(), IsEqual.equalTo(300L));
		Assert.assertThat(balances.getBalance(new BlockHeight(24L)).getNumMicroNem(), IsEqual.equalTo(302L));
		Assert.assertThat(balances.getBalance(new BlockHeight(19L)).getNumMicroNem(), IsEqual.equalTo(100L));
		Assert.assertThat(balances.getBalance(new BlockHeight(14L)).getNumMicroNem(), IsEqual.equalTo(101L));
		Assert.assertThat(balances.getBalance(new BlockHeight(9L)).getNumMicroNem(), IsEqual.equalTo(0L));
	
	}
	//endregion

	//region trim
	@Test
	public void historicalBalanceCanBeTrimmed() {
		// Arrange:
<<<<<<< HEAD
		BlockChain blockChain = new MockBlockChain();
		blockChain.getLastDbBlock().setHeight(1000L);
		final HistoricalBalances balances = new HistoricalBalances();
		balances.setblockChain(blockChain);
=======
		final BlockChainLastBlockLayer blockChainLastBlockLayer = mock(BlockChainLastBlockLayer.class);
		when(blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(1000L);
		final HistoricalBalances balances = new HistoricalBalances(blockChainLastBlockLayer);
>>>>>>> integration/20140501
		
		// Act:
		balances.add(new BlockHeight(100L), new Amount(1L));
		balances.add(new BlockHeight(200L), new Amount(2L));
		balances.add(new BlockHeight(300L), new Amount(4L));

		// Assert:
		Assert.assertThat(balances.size(), IsEqual.equalTo(3));

		// Act:
<<<<<<< HEAD
		blockChain.getLastDbBlock().setHeight(3000L);
=======
		when(blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(3000L);
>>>>>>> integration/20140501
		balances.add(new BlockHeight(2500L), new Amount(8L));

		// Assert:
		Assert.assertThat(balances.getBalance(new BlockHeight(2500L)).getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getBalance(new BlockHeight(2000L)).getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.size(), IsEqual.equalTo(2));
	}
	//endregion
<<<<<<< HEAD
}
=======

	private static HistoricalBalances createTestHistoricalBalances(long l) {
		final BlockChainLastBlockLayer blockChainLastBlockLayer = mock(BlockChainLastBlockLayer.class);
		when(blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(40L);
		return new HistoricalBalances(blockChainLastBlockLayer);
	}
}
>>>>>>> integration/20140501
