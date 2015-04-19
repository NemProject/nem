package org.nem.core.async;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.ExceptionUtils;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AsyncTimerTest {

	private static final int TimeUnit = 60;
	private static final int TimeHalfUnit = TimeUnit / 2;

	@Test
	public void initialDelayIsRespected() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, 10 * TimeUnit)) {
			// Arrange:
			Thread.sleep(TimeHalfUnit);

			// Assert:
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(0));

			// Arrange:
			Thread.sleep(3 * TimeHalfUnit);

			// Assert:
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void refreshIntervalIsRespectedWhenRecurrenceThrowsException() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture(() -> () -> {
			throw new RuntimeException("this shouldn't stop the timer");
		});
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, 2 * TimeUnit)) {
			// Arrange: (should fire at 1, 3, 5)
			Thread.sleep(6 * TimeUnit);

			// Assert:
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(3));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void refreshIntervalIsRespected() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, 2 * TimeUnit)) {
			// Arrange: (should fire at 1, 3, 5)
			Thread.sleep(6 * TimeUnit);

			// Assert:
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(3));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void refreshIntervalIsDerivedFromDelayStrategy() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		final MockDelayStrategy strategy = new MockDelayStrategy(new int[] { TimeUnit, 2 * TimeUnit, TimeUnit, 2 * TimeUnit });
		try (final AsyncTimer timer = createTimer(cf.getFutureSupplier(), TimeUnit, strategy, null)) {
			// Arrange: (should fire at 1, 2, 4, 5)
			Thread.sleep(6 * TimeUnit);

			// Assert:
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(4));
			Assert.assertThat(strategy.getNumNextCalls(), IsEqual.equalTo(4));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void closeStopsRefreshing() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, 2 * TimeUnit, visitor)) {
			// Arrange:
			Thread.sleep(3 * TimeHalfUnit);

			// Assert:
			Mockito.verify(visitor, Mockito.times(1)).notifyOperationStart();

			// Act:
			timer.close();
			Thread.sleep(9 * TimeHalfUnit);

			// Assert:
			Mockito.verify(visitor, Mockito.times(1)).notifyOperationStart();
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(true));
		}
	}

	@Test
	public void stoppedDelayStrategyStopsRefreshing() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);
		final AbstractDelayStrategy strategy = new UniformDelayStrategy(2 * TimeUnit, 1);
		try (final AsyncTimer timer = createTimer(cf.getFutureSupplier(), TimeUnit, strategy, visitor)) {
			// Arrange:
			Thread.sleep(3 * TimeHalfUnit);

			// Assert:
			Mockito.verify(visitor, Mockito.times(1)).notifyOperationStart();

			// Act:
			Thread.sleep(9 * TimeHalfUnit);

			// Assert:
			Mockito.verify(visitor, Mockito.times(1)).notifyOperationStart();
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(true));
		}
	}

	@Test
	public void timerThrottlesExecutions() throws InterruptedException {
		// Arrange:
		final Object refreshMonitor = new Object();
		final CountableFuture cf = new CountableFuture(() -> () -> Utils.monitorWait(refreshMonitor));
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, 2 * TimeUnit)) {
			// Arrange: (expect calls at 1, 3, 5)
			Thread.sleep(6 * TimeUnit);

			// Act: signal the monitor (one thread should be unblocked)
			Utils.monitorSignal(refreshMonitor);
			Thread.sleep(TimeHalfUnit);

			// Assert:
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void afterDelaysTimerUntilTriggerFires() throws InterruptedException {
		// Arrange:
		final CountableFuture cfTrigger = CountableFuture.sleep(3 * TimeUnit);
		try (final AsyncTimer triggerTimer = createTimer(cfTrigger, 2 * TimeUnit, 10 * TimeUnit)) {

			final CountableFuture cf = new CountableFuture();
			final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);
			try (final AsyncTimer timer = createTimerAfter(triggerTimer, cf, 10 * TimeUnit, visitor)) {
				// Arrange:
				Thread.sleep(3 * TimeUnit);

				// Assert:
				Mockito.verify(visitor, Mockito.times(0)).notifyOperationStart();
				Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(0));

				// Arrange:
				Thread.sleep(4 * TimeUnit);

				// Assert:
				Mockito.verify(visitor, Mockito.times(1)).notifyOperationStart();
				Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(1));
				Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
			}
		}
	}

	//region getFirstFireFuture

	@Test
	public void firstFireFutureIsSetAfterFirstSuccessfulCompletion() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();

		// Assert:
		assertFirstFireFutureIsSet(cf);
	}

	@Test
	public void firstFireFutureIsSetAfterFirstExceptionalCompletion() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture(() -> () -> {
			throw new RuntimeException("this shouldn't stop the timer");
		});

		// Assert:
		assertFirstFireFutureIsSet(cf);
	}

	private static void assertFirstFireFutureIsSet(final CountableFuture cf) throws InterruptedException {
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, 2 * TimeUnit)) {
			// Assert: initially unset
			Assert.assertThat(timer.getFirstFireFuture().isDone(), IsEqual.equalTo(false));

			// Arrange: (should fire at 1)
			Thread.sleep(2 * TimeUnit);

			// Assert: the future should be set after the initial fire
			Assert.assertThat(timer.getFirstFireFuture().isDone(), IsEqual.equalTo(true));

			// Arrange: (should fire at 3)
			Thread.sleep(2 * TimeUnit);

			// Assert: the future should be set after subsequent fires
			Assert.assertThat(timer.getFirstFireFuture().isDone(), IsEqual.equalTo(true));
		}
	}

	//endregion

	//region visitor

	@Test
	public void visitorIsNotifiedOfOperationStarts() throws InterruptedException {
		// Arrange:
		// Arrange:
		final CountableFuture cf = new CountableFuture(() -> () -> ExceptionUtils.propagateVoid(() -> Thread.sleep(2 * TimeUnit)));
		final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);
		try (final AsyncTimer timer = createTimer(cf, TimeHalfUnit, 10 * TimeUnit, visitor)) {
			// Arrange: (should fire at 0.5)
			Thread.sleep(TimeUnit);

			// Assert:
			Mockito.verify(visitor, Mockito.times(1)).notifyOperationStart();
			Mockito.verify(visitor, Mockito.times(0)).notifyOperationComplete();
			Mockito.verify(visitor, Mockito.times(0)).notifyOperationCompleteExceptionally(Mockito.any());
		}
	}

	@Test
	public void visitorIsNotifiedOfSuccessfulCompletions() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);
		try (final AsyncTimer timer = createTimer(cf, 2 * TimeUnit, 4 * TimeUnit, visitor)) {
			// Arrange: (should fire at 2, 6, 10)
			Thread.sleep(12 * TimeUnit);

			// Assert:
			Mockito.verify(visitor, Mockito.times(3)).notifyOperationStart();
			Mockito.verify(visitor, Mockito.times(3)).notifyOperationComplete();
			Mockito.verify(visitor, Mockito.times(0)).notifyOperationCompleteExceptionally(Mockito.any());
		}
	}

	@Test
	public void visitorIsNotifiedOfExceptionalCompletions() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture(() -> () -> {
			throw new RuntimeException("this shouldn't stop the timer");
		});
		final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, 2 * TimeUnit, visitor)) {
			// Arrange: (should fire at 1, 3, 5)
			Thread.sleep(6 * TimeUnit);

			// Assert:
			Mockito.verify(visitor, Mockito.times(3)).notifyOperationStart();
			Mockito.verify(visitor, Mockito.times(0)).notifyOperationComplete();
			Mockito.verify(visitor, Mockito.times(3)).notifyOperationCompleteExceptionally(Mockito.any());
		}
	}

	@Test
	public void visitorIsNotifiedOfDelays() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		final ArgumentCaptor<Integer> delayCaptor = ArgumentCaptor.forClass(Integer.class);
		final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);

		final MockDelayStrategy strategy = new MockDelayStrategy(new int[] { 2 * TimeUnit, 3 * TimeHalfUnit, TimeUnit });
		try (final AsyncTimer ignored = createTimer(cf.getFutureSupplier(), TimeHalfUnit, strategy, visitor)) {
			// Arrange: (should fire at 0.5, 2.5, 4.0, 5.0)
			Thread.sleep(6 * TimeUnit);

			// Assert:
			Mockito.verify(visitor, Mockito.times(4)).notifyDelay(delayCaptor.capture());
			Assert.assertThat(
					delayCaptor.getAllValues(),
					IsEqual.equalTo(Arrays.asList(TimeHalfUnit, 2 * TimeUnit, 3 * TimeHalfUnit, TimeUnit)));
		}
	}

	@Test
	public void visitorIsNotifiedOfStops() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, 2 * TimeUnit, visitor)) {
			// Arrange:
			Thread.sleep(3 * TimeHalfUnit);

			// Act:
			timer.close();
			Thread.sleep(9 * TimeHalfUnit);

			// Assert:
			Mockito.verify(visitor, Mockito.times(1)).notifyOperationStart();
			Mockito.verify(visitor, Mockito.times(1)).notifyStop();
		}
	}

	//endregion

	private static AsyncTimer createTimer(
			final CountableFuture cf,
			final int initialDelay,
			final int delay) {
		return createTimer(cf, initialDelay, delay, null);
	}

	private static AsyncTimer createTimer(
			final CountableFuture cf,
			final int initialDelay,
			final int delay,
			final AsyncTimerVisitor visitor) {
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder()
				.setRecurringFutureSupplier(cf.getFutureSupplier())
				.setInitialDelay(initialDelay)
				.setDelayStrategy(new UniformDelayStrategy(delay))
				.setVisitor(visitor)
				.create();
		return new AsyncTimer(options);
	}

	private static AsyncTimer createTimer(
			final Supplier<CompletableFuture<?>> recurringFutureSupplier,
			final int initialDelay,
			final AbstractDelayStrategy delay,
			final AsyncTimerVisitor visitor) {
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder()
				.setRecurringFutureSupplier(recurringFutureSupplier)
				.setInitialDelay(initialDelay)
				.setDelayStrategy(delay)
				.setVisitor(visitor)
				.create();
		return new AsyncTimer(options);
	}

	private static AsyncTimer createTimerAfter(
			final AsyncTimer triggerTimer,
			final CountableFuture cf,
			final int delay,
			final AsyncTimerVisitor visitor) {
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder()
				.setRecurringFutureSupplier(cf.getFutureSupplier())
				.setTrigger(triggerTimer.getFirstFireFuture())
				.setDelayStrategy(new UniformDelayStrategy(delay))
				.setVisitor(visitor)
				.create();
		return new AsyncTimer(options);
	}

	private static class CountableFuture {
		private int numCalls;
		private final Supplier<Runnable> runnableSupplier;

		public CountableFuture() {
			this.runnableSupplier = () -> () -> { };
		}

		public CountableFuture(final Supplier<Runnable> runnableSupplier) {
			this.runnableSupplier = runnableSupplier;
		}

		public Supplier<CompletableFuture<?>> getFutureSupplier() {
			return this::getFuture;
		}

		private CompletableFuture<?> getFuture() {
			return CompletableFuture.runAsync(() -> ++this.numCalls)
					.thenCompose(v -> CompletableFuture.runAsync(this.runnableSupplier.get()));
		}

		public int getNumCalls() {
			return this.numCalls;
		}

		private static CountableFuture sleep(final int milliseconds) {
			return new CountableFuture(() ->
					() -> ExceptionUtils.propagateVoid(() -> Thread.sleep(milliseconds)));
		}
	}

	private static class MockDelayStrategy extends AbstractDelayStrategy {

		private final int[] delays;
		private int numNextCalls;

		public MockDelayStrategy(final int[] delays) {
			this.delays = delays;
		}

		public int getNumNextCalls() {
			return this.numNextCalls;
		}

		@Override
		protected int nextInternal(final int iteration) {
			this.numNextCalls = iteration;
			return this.delays[iteration - 1];
		}
	}
}