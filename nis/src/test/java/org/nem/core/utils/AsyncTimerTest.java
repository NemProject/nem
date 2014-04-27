package org.nem.core.utils;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AsyncTimerTest {

	private static int TimeUnit = 25;
	private static int TimeHalfUnit = 25;

	//region get/setName

	@Test
	public void timerNameIsInitiallyUnset() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = new AsyncTimer(cf.getFutureSupplier(), TimeUnit, TimeUnit)) {
			// Assert:
			Assert.assertThat(timer.getName(), IsNull.nullValue());
		}
	}

	@Test
	public void timerNameCanBeSet() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = new AsyncTimer(cf.getFutureSupplier(), TimeUnit, TimeUnit)) {
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
		try (final AsyncTimer timer = new AsyncTimer(cf.getFutureSupplier(), TimeUnit, 10 * TimeUnit)) {
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
	public void refreshIntervalIsRespected() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = new AsyncTimer(cf.getFutureSupplier(), TimeUnit, 2 * TimeUnit)) {
			// Arrange: (should fire at 1, 3, 5)
			Thread.sleep(6 * TimeUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(3));
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(3));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void closeStopsRefreshing() throws InterruptedException {
		// Arrange:
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = new AsyncTimer(cf.getFutureSupplier(), TimeUnit, 2 * TimeUnit)) {
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
	public void timerThrottlesExecutions() throws InterruptedException {
		// Arrange:
		final Object refreshMonitor = new Object();
		final CountableFuture cf = new CountableFuture(() -> Utils.monitorWait(refreshMonitor));
		try (final AsyncTimer timer = new AsyncTimer(cf.getFutureSupplier(), TimeUnit, 2 * TimeUnit)) {
			// Arrange: (expect calls at 1, 3, 5)
			Thread.sleep(6 * TimeUnit);

			// Act: signal the monitor (one thread should be unblocked)
			Utils.monitorSignal(refreshMonitor);
			Thread.sleep(10 * TimeUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void afterDelaysTimerUntilTriggerFires() throws InterruptedException {
		//Arrange:
		final CompletableFuture<Void> triggerFuture = CompletableFuture.supplyAsync(() ->
				ExceptionUtils.propagate(() -> {
					Thread.sleep(5 * TimeUnit);
					return null;
				}));
		final CountableFuture cf = new CountableFuture();
		try (final AsyncTimer timer = AsyncTimer.After(triggerFuture, cf.getFutureSupplier(), 10 * TimeUnit)) {
			// Arrange:
			Thread.sleep(5 * TimeHalfUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(0));
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(0));

			// Arrange:
			Thread.sleep(15 * TimeHalfUnit);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
			Assert.assertThat(cf.getNumCalls(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	private static class CountableFuture {
		private int numCalls;
		private final Runnable runnable;

		public CountableFuture() {
			this.runnable = () -> ++this.numCalls;
		}

		public CountableFuture(final Runnable runnable) {
			this.runnable = runnable;
		}

		public Supplier<CompletableFuture<?>> getFutureSupplier() {
			return this::getFuture;
		}

		private CompletableFuture<Void> getFuture() {
			return CompletableFuture.runAsync(this.runnable);
		}

		public int getNumCalls() { return this.numCalls; }
	}
}