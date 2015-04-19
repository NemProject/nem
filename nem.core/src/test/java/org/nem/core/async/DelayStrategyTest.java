package org.nem.core.async;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

public class DelayStrategyTest {

	//region AbstractDelayStrategy

	@Test(expected = IllegalStateException.class)
	public void cannotCallNextOnStoppedStrategy() {
		// Arrange:
		final UniformDelayStrategy strategy = new UniformDelayStrategy(41, 1);
		strategy.next();

		// Assert:
		Assert.assertThat(strategy.shouldStop(), IsEqual.equalTo(true));

		// Act:
		strategy.next();
	}

	//endregion

	//region UniformDelayStrategy

	@Test
	public void infiniteUniformStrategyReturnsSameValueForever() {
		// Arrange:
		final UniformDelayStrategy strategy = new UniformDelayStrategy(41);

		// Assert:
		for (int i = 0; i < 100; ++i) {
			final String message = "Iteration: " + i;
			Assert.assertThat(message, strategy.shouldStop(), IsEqual.equalTo(false));
			Assert.assertThat(message, strategy.next(), IsEqual.equalTo(41));
		}
	}

	@Test
	public void finiteUniformStrategyReturnsSameValueUpToLimit() {
		// Arrange:
		final UniformDelayStrategy strategy = new UniformDelayStrategy(41, 3);

		// Assert:
		assertStrategy(strategy, new int[] { 41, 41, 41 });
	}

	//endregion

	//region LinearDelayStrategy

	@Test
	public void linearStrategyReturnsIncreasingDelays() {
		// Arrange:
		final LinearDelayStrategy strategy = new LinearDelayStrategy(10, 70, 4);

		// Assert:
		assertStrategy(strategy, new int[] { 10, 30, 50, 70 });
	}

	@Test
	public void linearStrategyCanBeCreatedWithApproximateDuration() {
		// Assert:
		assertStrategy(LinearDelayStrategy.withDuration(10, 70, 80), new int[] { 10, 70 }); // sum == 80
		assertStrategy(LinearDelayStrategy.withDuration(10, 70, 160), new int[] { 10, 30, 50, 70 }); // sum == 160
		assertStrategy(LinearDelayStrategy.withDuration(10, 70, 200), new int[] { 10, 25, 40, 55, 70 }); // sum == 200
		assertStrategy(LinearDelayStrategy.withDuration(10, 70, 300), new int[] { 10, 20, 30, 40, 50, 60, 70 }); // sum == 280
	}

	@Test
	public void linearStrategyRequiresDurationAtLeastSumOfMinDelayAndMaxDelay() {
		// Assert:
		ExceptionAssert.assertThrows(v -> LinearDelayStrategy.withDuration(10, 70, 0), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> LinearDelayStrategy.withDuration(10, 70, 1), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> LinearDelayStrategy.withDuration(10, 70, 79), IllegalArgumentException.class);
	}

	//endregion

	//region AggregateDelayStrategy

	@Test
	public void aggregateStrategyDelegatesToEachSubStrategyInOrderUntilExhaustion() {
		// Arrange:
		final List<AbstractDelayStrategy> subStrategies = Arrays.asList(
				new UniformDelayStrategy(7, 2),
				new UniformDelayStrategy(4, 1),
				new UniformDelayStrategy(9, 0),
				new UniformDelayStrategy(1, 1));
		final AggregateDelayStrategy strategy = new AggregateDelayStrategy(subStrategies);

		// Assert:
		assertStrategy(strategy, new int[] { 7, 7, 4, 1 });
	}

	//endregion

	private static void assertStrategy(final AbstractDelayStrategy strategy, final int[] expectedDelays) {
		for (final int expectedDelay : expectedDelays) {
			Assert.assertThat(strategy.shouldStop(), IsEqual.equalTo(false));
			Assert.assertThat(strategy.next(), IsEqual.equalTo(expectedDelay));
		}

		Assert.assertThat(strategy.shouldStop(), IsEqual.equalTo(true));
	}
}
