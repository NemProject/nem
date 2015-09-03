package org.nem.core.async;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.util.concurrent.CompletableFuture;

public class SleepFutureTest {

	private static final int TIME_UNIT = 30;
	private static final int DELTA = 10;

	@Test
	public void sleepFutureIsInitiallyNotCompleted() throws InterruptedException {
		// Arrange:
		final CompletableFuture<?> future = SleepFuture.create(TIME_UNIT);

		// Assert:
		Assert.assertThat(future.isDone(), IsEqual.equalTo(false));
	}

	@Test
	public void sleepFutureIsCompletedAfterDelay() throws InterruptedException {
		// Arrange:
		final CompletableFuture<?> future = SleepFuture.create(TIME_UNIT);

		Thread.sleep(TIME_UNIT + DELTA);

		// Assert:
		Assert.assertThat(future.isDone(), IsEqual.equalTo(true));
	}

	@Test
	public void sleepFuturesAreExecutedConcurrently() throws InterruptedException {
		// Arrange:
		final CompletableFuture<?>[] futures = new CompletableFuture[100];
		for (int i = 0; i < futures.length; ++i) {
			futures[i] = SleepFuture.create(TIME_UNIT);
		}

		Thread.sleep(TIME_UNIT + DELTA);

		// Assert:
		for (final CompletableFuture<?> future : futures) {
			Assert.assertThat(future.isDone(), IsEqual.equalTo(true));
		}
	}
}