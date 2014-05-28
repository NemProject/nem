package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.util.concurrent.CompletableFuture;

public class NisPeerNetworkHostTest {

	@Test
	public void defaultHostCanBeBooted() {
		// Arrange:
		try (final NisPeerNetworkHost host = new NisPeerNetworkHost(null, null)) {
			// Act:
			host.boot().join();
		}
	}

	@Test
	public void defaultHostCanBeBootedAsync() {
		// Arrange:
		try (final NisPeerNetworkHost host = new NisPeerNetworkHost(null, null)) {
			// Act:
			final CompletableFuture future = host.boot();

			// Assert:
			Assert.assertThat(future.isDone(), IsEqual.equalTo(false));

			// Cleanup:
			future.join();
		}
	}
}
