package org.nem.core.model;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;

import java.util.function.Supplier;

public class DefaultTransactionFeeCalculatorTest {

	// region calculateMinimumFee

	@Test
	public void calculatorDelegatesToBeforeForkMinimumFeeCalculatorForHeightsBeforeForkHeight() {
		assertMinimumFeeCalculatorBeforeFork(1L, 1234L);
		assertMinimumFeeCalculatorBeforeFork(10L, 1234L);
		assertMinimumFeeCalculatorBeforeFork(100L, 1234L);
		assertMinimumFeeCalculatorBeforeFork(1000L, 1234L);
		assertMinimumFeeCalculatorBeforeFork(1233L, 1234L);
	}

	@Test
	public void calculatorDelegatesToAfterForkMinimumFeeCalculatorAtForkHeight() {
		assertMinimumFeeCalculatorAfterFork(1L, 1L);
		assertMinimumFeeCalculatorAfterFork(10L, 10L);
		assertMinimumFeeCalculatorAfterFork(100L, 100L);
		assertMinimumFeeCalculatorAfterFork(1000L, 1000L);
		assertMinimumFeeCalculatorAfterFork(1234L, 1234L);
	}

	@Test
	public void calculatorDelegatesToAfterForkMinimumFeeCalculatorForHeightsAfterForkHeight() {
		assertMinimumFeeCalculatorAfterFork(2L, 1L);
		assertMinimumFeeCalculatorAfterFork(1235L, 1234L);
		assertMinimumFeeCalculatorAfterFork(2000L, 1234L);
		assertMinimumFeeCalculatorAfterFork(10000L, 1234L);
		assertMinimumFeeCalculatorAfterFork(1_000_000L, 1234L);
	}

	private static void assertMinimumFeeCalculatorBeforeFork(final long testHeight, final long forkHeight) {
		// Arrange:
		final TestContext context = new TestContext(() -> new BlockHeight(testHeight), new BlockHeight(forkHeight));
		final Transaction transaction = Mockito.mock(TransferTransaction.class);

		// Act:
		context.calculator.calculateMinimumFee(transaction);

		// Assert:
		Mockito.verify(context.calculatorBeforeFork, Mockito.only()).calculateMinimumFee(transaction);
		Mockito.verify(context.calculatorAfterFork, Mockito.never()).calculateMinimumFee(Mockito.any(TransferTransaction.class));
	}

	private static void assertMinimumFeeCalculatorAfterFork(final long testHeight, final long forkHeight) {
		// Arrange:
		final TestContext context = new TestContext(() -> new BlockHeight(testHeight), new BlockHeight(forkHeight));
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		context.calculator.calculateMinimumFee(transaction);

		// Assert:
		Mockito.verify(context.calculatorBeforeFork, Mockito.never()).calculateMinimumFee(Mockito.any());
		Mockito.verify(context.calculatorAfterFork, Mockito.only()).calculateMinimumFee(transaction);
	}

	// endregion

	// region isFeeValid

	@Test
	public void calculatorDelegatesToBeforeForkIsFeeValidCalculatorForHeightsBeforeForkHeight() {
		assertIsFeeValidCalculatorBeforeFork(1L, 1234L);
		assertIsFeeValidCalculatorBeforeFork(10L, 1234L);
		assertIsFeeValidCalculatorBeforeFork(100L, 1234L);
		assertIsFeeValidCalculatorBeforeFork(1000L, 1234L);
		assertIsFeeValidCalculatorBeforeFork(1233L, 1234L);
	}

	@Test
	public void calculatorDelegatesToAfterForkIsFeeValidCalculatorAtForkHeight() {
		assertIsFeeValidCalculatorAfterFork(1L, 1L);
		assertIsFeeValidCalculatorAfterFork(10L, 10L);
		assertIsFeeValidCalculatorAfterFork(100L, 100L);
		assertIsFeeValidCalculatorAfterFork(1000L, 1000L);
		assertIsFeeValidCalculatorAfterFork(1234L, 1234L);
	}

	@Test
	public void calculatorDelegatesToAfterForkIsFeeValidCalculatorForHeightsAfterForkHeight() {
		assertIsFeeValidCalculatorAfterFork(2L, 1L);
		assertIsFeeValidCalculatorAfterFork(1235L, 1234L);
		assertIsFeeValidCalculatorAfterFork(2000L, 1234L);
		assertIsFeeValidCalculatorAfterFork(10000L, 1234L);
		assertIsFeeValidCalculatorAfterFork(1_000_000L, 1234L);
	}

	private static void assertIsFeeValidCalculatorBeforeFork(final long testHeight, final long forkHeight) {
		// Arrange:
		final TestContext context = new TestContext(() -> new BlockHeight(testHeight), new BlockHeight(forkHeight));
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		context.calculator.isFeeValid(transaction, new BlockHeight(testHeight));

		// Assert:
		Mockito.verify(context.calculatorBeforeFork, Mockito.only()).isFeeValid(transaction, new BlockHeight(testHeight));
		Mockito.verify(context.calculatorAfterFork, Mockito.never()).isFeeValid(Mockito.any(), Mockito.any());
	}

	private static void assertIsFeeValidCalculatorAfterFork(final long testHeight, final long forkHeight) {
		// Arrange:
		final TestContext context = new TestContext(() -> new BlockHeight(testHeight), new BlockHeight(forkHeight));
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		context.calculator.isFeeValid(transaction, new BlockHeight(testHeight));

		// Assert:
		Mockito.verify(context.calculatorBeforeFork, Mockito.never()).isFeeValid(Mockito.any(), Mockito.any());
		Mockito.verify(context.calculatorAfterFork, Mockito.only()).isFeeValid(transaction, new BlockHeight(testHeight));
	}

	// endregion

	private static class TestContext {
		private final TransactionFeeCalculatorBeforeFork calculatorBeforeFork = Mockito.mock(TransactionFeeCalculatorBeforeFork.class);
		private final TransactionFeeCalculatorAfterFork calculatorAfterFork = Mockito.mock(TransactionFeeCalculatorAfterFork.class);
		private final DefaultTransactionFeeCalculator calculator;

		public TestContext(final Supplier<BlockHeight> heightSupplier, final BlockHeight forkHeight) {
			this.calculator = new DefaultTransactionFeeCalculator(
					heightSupplier,
					forkHeight,
					this.calculatorBeforeFork,
					this.calculatorAfterFork);
		}
	}
}
