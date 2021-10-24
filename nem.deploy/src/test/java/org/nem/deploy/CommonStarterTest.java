package org.nem.deploy;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

public class CommonStarterTest {

	//region stopServerAsync

	@Test
	public void stopServerAsyncDoesNotImmediatelyStopServer() {
		// Arrange:
		final CommonStarter starter = Mockito.spy(new CommonStarter());

		// Act:
		final CompletableFuture<?> future = starter.stopServerAsync();

		// Assert:
		MatcherAssert.assertThat(future.isDone(), IsEqual.equalTo(false));

		// Cleanup:
		future.join();
	}

	@Test
	public void stopServerAsyncDelegatesToStopServer() {
		// Arrange:
		final CommonStarter starter = Mockito.spy(new CommonStarter());

		// Act:
		final CompletableFuture<Boolean> future = starter.stopServerAsync();
		final boolean result = future.join();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
		Mockito.verify(starter, Mockito.times(1)).stopServer();
	}

	@Test
	public void stopServerAsyncFutureIsCompletedOnFailure() {
		// Arrange:
		final CommonStarter starter = Mockito.spy(new CommonStarter());
		Mockito.doThrow(new RuntimeException()).when(starter).stopServer();

		// Act:
		final CompletableFuture<Boolean> future = starter.stopServerAsync();
		final boolean result = future.join();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(false));
		Mockito.verify(starter, Mockito.times(1)).stopServer();
	}

	//endregion
}