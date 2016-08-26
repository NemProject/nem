package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.TestTransactionRegistry;

import java.util.Collection;
import java.util.stream.Collectors;

public class ZeroTransactionFeeCalculatorTest {

	@Test
	public void calculatorAlwaysReturnsAmountZero() {
		// Arrange:
		final ZeroTransactionFeeCalculator calculator = new ZeroTransactionFeeCalculator();

		// Act:
		final Collection<Amount> amounts = TestTransactionRegistry.stream()
				.map(entry -> calculator.calculateMinimumFee(entry.createModel.get()))
				.collect(Collectors.toList());

		// Assert:
		amounts.forEach(amount -> Assert.assertThat(amount, IsEqual.equalTo(Amount.ZERO)));
	}

	@Test
	public void calculatorAcceptsTransactionsWithZeroFee() {
		// Arrange:
		final ZeroTransactionFeeCalculator calculator = new ZeroTransactionFeeCalculator();
		final Collection<Transaction> transactions = TestTransactionRegistry.stream()
				.map(entry -> {
					final Transaction transaction = entry.createModel.get();
					transaction.setFee(Amount.ZERO);
					return transaction;
				})
				.collect(Collectors.toList());

		// Assert:
		transactions.forEach(t -> Assert.assertThat(
				calculator.isFeeValid(t, new BlockHeight(123)),
				IsEqual.equalTo(true)));
	}

	@Test
	public void calculatorAcceptsTransactionsWithNonZeroFee() {
		// Arrange:
		final ZeroTransactionFeeCalculator calculator = new ZeroTransactionFeeCalculator();
		final Collection<Transaction> transactions = TestTransactionRegistry.stream()
				.map(entry -> {
					final Transaction transaction = entry.createModel.get();
					transaction.setFee(new Amount(12345));
					return transaction;
				})
				.collect(Collectors.toList());

		// Assert:
		transactions.forEach(t -> Assert.assertThat(
				calculator.isFeeValid(t, new BlockHeight(123)),
				IsEqual.equalTo(true)));
	}
}
