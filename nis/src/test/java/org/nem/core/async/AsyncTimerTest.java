package org.nem.core.async;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.ExceptionUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AsyncTimerTest {

	private static int TimeUnit = 40;
	private static int TimeHalfUnit = TimeUnit / 2;

	//region get/setName

	@Test
	public void timerNameIsInitiallyUnset() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, TimeUnit)) {
			// Assert:
			Assert.assertThat(timer.getName(), IsNull.nullValue());
		}
	}

	@Test
	public void timerNameCanBeSet() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, TimeUnit)) {
			// Act:
			timer.setName("AlphaGamma");

			// Assert:
			Assert.assertThat(timer.getName(), IsEqual.equalTo("AlphaGamma"));
		}
	}

	//endregion

	@Test
	public void initialDelayIsRespected() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, 10 * TimeUnit)) {
			// Arrange:
			Thread.sleep(TimeHalfUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(0));
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(0));

			// Arrange:
			Thread.sleep(3 * TimeHalfUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
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
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(3));
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
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(3));
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(3));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void refreshIntervalIsDerivedFromDelayStrategy() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		final MockDelayStrategy strategy = new MockDelayStrategy(new int[] { TimeUnit, 2*TimeUnit, TimeUnit, 2*TimeUnit });
		try (final AsyncTimer timer = new AsyncTimer(cf.getFutureSupplier(), TimeUnit, strategy)) {
			// Arrange: (should fire at 1, 2, 4, 5)
			Thread.sleep(6 * TimeUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(4));
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(4));
			Assert.assertThat(strategy.getNumNextCalls(), IsEqual.equalTo(4));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void closeStopsRefreshing() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = createTimer(cf, TimeUnit, 2 * TimeUnit)) {
			// Arrange:
			Thread.sleep(3 * TimeHalfUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));

			// Act:
			timer.close();
			Thread.sleep(9 * TimeHalfUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(true));
		}
	}

	@Test
	public void stoppedDelayStrategyStopsRefreshing() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		final AbstractDelayStrategy strategy = new UniformDelayStrategy(2 * TimeUnit, 1);
		try (final AsyncTimer timer = new AsyncTimer(cf.getFutureSupplier(), TimeUnit, strategy)) {
			// Arrange:
			Thread.sleep(3 * TimeHalfUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));

			// Act:
			Thread.sleep(9 * TimeHalfUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
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
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void afterDelaysTimerUntilTriggerFires() throws InterruptedException {
		// Arrange:
		final CountableFuture cfTrigger = CountableFuture.sleep(3 * TimeUnit);
		try (final AsyncTimer triggerTimer = createTimer(cfTrigger, 2 * TimeUnit, 10 * TimeUnit)) {
			triggerTimer.setName("TRIGGER");

			final CountableFuture cf = new CountableFuture();
			try (final AsyncTimer timer = createTimerAfter(triggerTimer, cf, 10 * TimeUnit)) {
				timer.setName("SUB TIMER");

				// Arrange:
				Thread.sleep(3 * TimeUnit);

				// Assert:
				Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(0));
				Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(0));

				// Arrange:
				Thread.sleep(4 * TimeUnit);

				// Assert:
				Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
				Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(1));
				Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
			}
		}
	}

	private static AsyncTimer createTimer(final CountableFuture cf, final int initialDelay, final int delay) {
		return new AsyncTimer(cf.getFutureSupplier(), initialDelay, new UniformDelayStrategy(delay));
	}

	private static AsyncTimer createTimerAfter(final AsyncTimer triggerTimer, final CountableFuture cf, final int delay) {
		return AsyncTimer.After(triggerTimer, cf.getFutureSupplier(), new UniformDelayStrategy(delay));
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
					.thenCompose(v -> CompletableFuture.runAsync(runnableSupplier.get()));
		}

		public int getNumCalls() { return this.numCalls; }

		private static CountableFuture sleep(int milliseconds) {
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
		protected int nextInternal(int iteration) {
			this.numNextCalls = iteration;
			return this.delays[iteration - 1];
		}
	}
}