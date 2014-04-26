package org.nem.core.utils;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import java.util.concurrent.CompletableFuture;

public class AsyncTimerTest {

	@Test
	public void timerNameIsInitiallyUnset() throws InterruptedException {
		// Arrange:
		final CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> null);
		try (final AsyncTimer timer = new AsyncTimer(future, 50, 100)) {
			// Assert:
			Assert.assertThat(timer.getName(), IsNull.nullValue());
		}
	}

	@Test
	public void timerNameCanBeSet() throws InterruptedException {
		// Arrange:
		final CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> null);
		try (final AsyncTimer timer = new AsyncTimer(future, 50, 100)) {
			// Act:
			timer.setName("AlphaGamma");

			// Assert:
			Assert.assertThat(timer.getName(), IsEqual.equalTo("AlphaGamma"));
		}
	}

	@Test
	public void initialRefreshRateIsRespected() throws InterruptedException {
		// Arrange:
		final CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> null);
		try (final AsyncTimer timer = new AsyncTimer(future, 50, 100)) {
			// Arrange:
			Thread.sleep(25);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(0));

			// Arrange:
			Thread.sleep(75);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void refreshIntervalIsRespected() throws InterruptedException {
		// Arrange:
		final CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> null);
		try (final AsyncTimer timer = new AsyncTimer(future, 10, 20)) {
			// Arrange:
			Thread.sleep(60);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(3));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void closeStopsRefreshing() throws InterruptedException {
		// Arrange:
		final CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> null);
		try (final AsyncTimer timer = new AsyncTimer(future, 10, 20)) {
			// Arrange:
			Thread.sleep(15);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));

			// Act:
			timer.close();
			Thread.sleep(45);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(true));
		}
	}

	@Test
	public void timerThrottlesExecutions() throws InterruptedException {
		// Arrange:
		final Object refreshMonitor = new Object();
		final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> Utils.monitorWait(refreshMonitor));
		try (final AsyncTimer timer = new AsyncTimer(future, 10, 20)) {
			// Arrange: (expect calls at 10, 30, 50)
			Thread.sleep(60);

			// Act: signal the monitor (one thread should be unblocked)
			Utils.monitorSignal(refreshMonitor);
			Thread.sleep(5);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}

	@Test
	public void afterDelaysTimerUntilTriggerFires() throws InterruptedException {
		//Arrange:
		final CompletableFuture<Void> triggerFuture = CompletableFuture.supplyAsync(() ->
				ExceptionUtils.propagate(() -> {
					Thread.sleep(50);
					return null;
				}));
		final CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> null);
		try (final AsyncTimer timer = AsyncTimer.After(triggerFuture, future, 100)) {
			// Arrange:
			Thread.sleep(25);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(0));

			// Arrange:
			Thread.sleep(75);

			// Assert:
			Assert.assertThat(timer.getNumExecutions(), IsEqual.equalTo(1));
			Assert.assertThat(timer.isStopped(), IsEqual.equalTo(false));
		}
	}
}