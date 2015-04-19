package org.nem.core.async;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.util.concurrent.CompletableFuture;

public class SleepFutureTest {

	private static final int TimeUnit = 30;
	private static final int Delta = 10;

	@Test
	public void sleepFutureIsInitiallyNotCompleted() throws InterruptedException {
		// Arrange:
		final CompletableFuture<?> future = SleepFuture.create(TimeUnit);

		// Assert:
		Assert.assertThat(future.isDone(), IsEqual.equalTo(false));
	}

	@Test
	public void sleepFutureIsCompletedAfterDelay() throws InterruptedException {
		// Arrange:
		final CompletableFuture<?> future = SleepFuture.create(TimeUnit);

		Thread.sleep(TimeUnit + Delta);

		// Assert:
		Assert.assertThat(future.isDone(), IsEqual.equalTo(true));
	}

	@Test
	public void sleepFuturesAreExecutedConcurrently() throws InterruptedException {
		// Arrange:
		final CompletableFuture<?>[] futures = new CompletableFuture[100];
		for (int i = 0; i < futures.length; ++i) {
			futures[i] = SleepFuture.create(TimeUnit);
		}

		Thread.sleep(TimeUnit + Delta);

		// Assert:
		for (final CompletableFuture<?> future : futures) {
			Assert.assertThat(future.isDone(), IsEqual.equalTo(true));
		}
	}
}