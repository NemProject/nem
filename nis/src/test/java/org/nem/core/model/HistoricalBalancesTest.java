package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;

import java.security.InvalidParameterException;

public class HistoricalBalancesTest {

	//region Copy
	@Test
	public void historicalBalanceCanBeCopied() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances();
		final BlockHeight lastBlockHeight = new BlockHeight(40);

		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L));
		balances.add(new BlockHeight(20L), new Amount(2L));
		balances.add(new BlockHeight(25L), new Amount(4L));
		balances.add(new BlockHeight(30L), new Amount(8L));
		HistoricalBalances balances2 = balances.copy();

		// Assert:
		Assert.assertThat(balances2.getBalance(lastBlockHeight, new BlockHeight(31L)).getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances2.getBalance(lastBlockHeight, new BlockHeight(30L)).getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances2.getBalance(lastBlockHeight, new BlockHeight(29L)).getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances2.getBalance(lastBlockHeight, new BlockHeight(25L)).getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances2.getBalance(lastBlockHeight, new BlockHeight(24L)).getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances2.getBalance(lastBlockHeight, new BlockHeight(20L)).getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances2.getBalance(lastBlockHeight, new BlockHeight(19L)).getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances2.getBalance(lastBlockHeight, new BlockHeight(10L)).getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances2.getBalance(lastBlockHeight, new BlockHeight(9L)).getNumMicroNem(), IsEqual.equalTo(0L));
	}

	@Test
	public void copyingEmptyReturnsEmpty() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances();

		// Act:
		final HistoricalBalances result = balances.copy();

		// Assert:
		Assert.assertThat(result.size(), IsEqual.equalTo(0));
		Assert.assertThat(result, IsNot.not(IsSame.sameInstance(balances)));
	}
	//endregion
	
	//region Retrieval
	@Test
	public void historicalBalanceCanBeRetrieved() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances();
		final BlockHeight lastBlockHeight = new BlockHeight(40);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L));
		balances.add(new BlockHeight(20L), new Amount(2L));
		balances.add(new BlockHeight(25L), new Amount(4L));
		balances.add(new BlockHeight(30L), new Amount(8L));

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(31L)).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(30L)).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(29L)).getBalance().getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(25L)).getBalance().getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(24L)).getBalance().getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(20L)).getBalance().getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(19L)).getBalance().getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(10L)).getBalance().getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(9L)).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
	}

	@Test
	public void balanceCanBeRetrieved() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances();
		final BlockHeight lastBlockHeight = new BlockHeight(40);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L));
		balances.add(new BlockHeight(20L), new Amount(2L));
		balances.add(new BlockHeight(25L), new Amount(4L));
		balances.add(new BlockHeight(30L), new Amount(8L));

		// Assert:
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(31L)).getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(30L)).getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(29L)).getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(25L)).getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(24L)).getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(20L)).getNumMicroNem(), IsEqual.equalTo(3L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(19L)).getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(10L)).getNumMicroNem(), IsEqual.equalTo(1L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(9L)).getNumMicroNem(), IsEqual.equalTo(0L));
	}

	@Test(expected = InvalidParameterException.class)
	public void retrivingFromThePastThrowsException() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances();
		final BlockHeight lastBlockHeight = new BlockHeight(4000);

		// Act:
		balances.add(new BlockHeight(4000L), new Amount(123L));

		// Assert:
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(4000L)).getNumMicroNem(), IsEqual.equalTo(123L));

		balances.getBalance(lastBlockHeight, new BlockHeight(1000L));
	}

	@Test(expected = InvalidParameterException.class)
	public void retrivingFromTheFutureThrowsException() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances();
		final BlockHeight lastBlockHeight = new BlockHeight(4000);

		// Act:
		balances.add(new BlockHeight(4000L), new Amount(123L));

		// Assert:
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(4000L)).getNumMicroNem(), IsEqual.equalTo(123L));
		balances.getBalance(lastBlockHeight, new BlockHeight(4001L));
	}
	//endregion


	//region add/subtract
	@Test
	public void historicalBalanceCanBeAdded() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances();
		final BlockHeight lastBlockHeight = new BlockHeight(40);

		// Act:
		balances.add(new BlockHeight(10L), new Amount(1L));
		balances.add(new BlockHeight(20L), new Amount(2L));
		balances.add(new BlockHeight(30L), new Amount(4L));
		balances.add(new BlockHeight(10L), new Amount(100L));
		balances.add(new BlockHeight(20L), new Amount(200L));
		balances.add(new BlockHeight(30L), new Amount(400L));

		// Assert:
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(31L)).getNumMicroNem(), IsEqual.equalTo(707L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(30L)).getNumMicroNem(), IsEqual.equalTo(707L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(20L)).getNumMicroNem(), IsEqual.equalTo(303L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(10L)).getNumMicroNem(), IsEqual.equalTo(101L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(5L)).getNumMicroNem(), IsEqual.equalTo(0L));
	
	}

	@Test
	public void historicalBalanceCanBeSubtracted() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances();
		final BlockHeight lastBlockHeight = new BlockHeight(40);
		
		// Act:
		balances.add(new BlockHeight(10L), new Amount(101L));
		balances.add(new BlockHeight(20L), new Amount(202L));
		balances.add(new BlockHeight(30L), new Amount(404L));
		balances.subtract(new BlockHeight(10L), new Amount(1L));
		balances.subtract(new BlockHeight(20L), new Amount(2L));
		balances.subtract(new BlockHeight(30L), new Amount(4L));

		// Assert:
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(31L)).getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(30L)).getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(20L)).getNumMicroNem(), IsEqual.equalTo(300L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(10L)).getNumMicroNem(), IsEqual.equalTo(100L));
		Assert.assertThat(balances.getBalance(lastBlockHeight, new BlockHeight(5L)).getNumMicroNem(), IsEqual.equalTo(0L));
	
	}

	@Test
	public void historicalBalanceCanBeSubtracted2() {
		// Arrange:
		final HistoricalBalances balances = createTestHistoricalBalances();
		final BlockHeight lastBlockHeight = new BlockHeight(40);

		// Act:
		balances.add(new BlockHeight(10L), new Amount(101L));
		balances.add(new BlockHeight(20L), new Amount(202L));
		balances.add(new BlockHeight(30L), new Amount(404L));
		balances.subtract(new BlockHeight(15L), new Amount(1L));
		balances.subtract(new BlockHeight(25L), new Amount(2L));
		balances.subtract(new BlockHeight(35L), new Amount(4L));

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(35L)).getBalance().getNumMicroNem(), IsEqual.equalTo(700L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(34L)).getBalance().getNumMicroNem(), IsEqual.equalTo(704L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(29L)).getBalance().getNumMicroNem(), IsEqual.equalTo(300L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(24L)).getBalance().getNumMicroNem(), IsEqual.equalTo(302L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(19L)).getBalance().getNumMicroNem(), IsEqual.equalTo(100L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(14L)).getBalance().getNumMicroNem(), IsEqual.equalTo(101L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(9L)).getBalance().getNumMicroNem(), IsEqual.equalTo(0L));
	
	}
	//endregion

	//region trim
	@Test
	public void historicalBalanceCanBeTrimmed() {
		// Arrange:
		final HistoricalBalances balances = new HistoricalBalances();
		final BlockHeight lastBlockHeight = new BlockHeight(3000);
		
		// Act:
		balances.add(new BlockHeight(100L), new Amount(1L));
		balances.add(new BlockHeight(200L), new Amount(2L));
		balances.add(new BlockHeight(300L), new Amount(4L));

		// Assert:
		Assert.assertThat(balances.size(), IsEqual.equalTo(3));

		// Act:
		balances.add(new BlockHeight(2500L), new Amount(8L));

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(2500L)).getBalance().getNumMicroNem(), IsEqual.equalTo(15L));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(2000L)).getBalance().getNumMicroNem(), IsEqual.equalTo(7L));
		Assert.assertThat(balances.size(), IsEqual.equalTo(2));
	}

	@Test
	public void trimmingReturnsCorrectResults() {
		// Arrange:
		final HistoricalBalances balances = new HistoricalBalances();
		final BlockHeight lastBlockHeight = new BlockHeight(10000);

		// Act:
		balances.add(new BlockHeight(100L), Amount.fromNem(1));
		balances.add(new BlockHeight(3100L), Amount.fromNem(1));
		balances.add(new BlockHeight(6100L), Amount.fromNem(1));
		balances.add(new BlockHeight(9100L), Amount.fromNem(1));

		// Assert:
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(9000)).getBalance(), IsEqual.equalTo(Amount.fromNem(3)));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(9100)).getBalance(), IsEqual.equalTo(Amount.fromNem(4)));
		Assert.assertThat(balances.getHistoricalBalance(lastBlockHeight, new BlockHeight(10000)).getBalance(), IsEqual.equalTo(Amount.fromNem(4)));
		Assert.assertThat(balances.size(), IsEqual.equalTo(2));
	}
	//endregion

	private static HistoricalBalances createTestHistoricalBalances() {
		return new HistoricalBalances();
	}
}
