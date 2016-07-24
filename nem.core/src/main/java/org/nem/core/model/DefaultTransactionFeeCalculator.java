package org.nem.core.model;

import org.nem.core.model.mosaic.MosaicFeeInformationLookup;
import org.nem.core.model.primitive.*;

import java.util.function.Supplier;

/**
 * Defualt implementation for calculating and validating transaction fees.
 */
public class DefaultTransactionFeeCalculator implements TransactionFeeCalculator {
	private final Supplier<BlockHeight> heightSupplier;
	private final BlockHeight forkHeight;
	private final TransactionFeeCalculatorBeforeFork calculatorBeforeFork;
	private final TransactionFeeCalculatorAfterFork calculatorAfterFork;

	public DefaultTransactionFeeCalculator(
			final MosaicFeeInformationLookup mosaicFeeInformationLookup,
			final Supplier<BlockHeight> heightSupplier,
			final BlockHeight forkHeight) {
		this(
				heightSupplier,
				forkHeight,
				new TransactionFeeCalculatorBeforeFork(mosaicFeeInformationLookup),
				new TransactionFeeCalculatorAfterFork(mosaicFeeInformationLookup));
	}

	public DefaultTransactionFeeCalculator(
			final Supplier<BlockHeight> heightSupplier,
			final BlockHeight forkHeight,
			final TransactionFeeCalculatorBeforeFork calculatorBeforeFork,
			final TransactionFeeCalculatorAfterFork calculatorAfterFork) {
		this.heightSupplier = heightSupplier;
		this.forkHeight = forkHeight;
		this.calculatorBeforeFork = calculatorBeforeFork;
		this.calculatorAfterFork = calculatorAfterFork;
	}

	@Override
	public Amount calculateMinimumFee(Transaction transaction) {
		return this.heightSupplier.get().compareTo(this.forkHeight) < 0
				? this.calculatorBeforeFork.calculateMinimumFee(transaction)
				: this.calculatorAfterFork.calculateMinimumFee(transaction);
	}

	@Override
	public boolean isFeeValid(Transaction transaction, BlockHeight blockHeight) {
		return blockHeight.compareTo(this.forkHeight) < 0
				? this.calculatorBeforeFork.isFeeValid(transaction, blockHeight)
				: this.calculatorAfterFork.isFeeValid(transaction, blockHeight);
	}
}
