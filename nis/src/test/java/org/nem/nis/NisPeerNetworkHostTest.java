package org.nem.nis;

import org.junit.*;

public class NisPeerNetworkHostTest {

	@Test
	public void defaultHostCanBeBooted() {
		// Arrange:
		try (final NisPeerNetworkHost host = new NisPeerNetworkHost()) {
			// Act:
			host.boot();
		}
	}
}
