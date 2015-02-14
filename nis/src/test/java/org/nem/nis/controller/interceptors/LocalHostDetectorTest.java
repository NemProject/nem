package org.nem.nis.controller.interceptors;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.ExceptionAssert;

import javax.servlet.http.*;

public class LocalHostDetectorTest {

	@Test
	public void localIpv4AddressIsDetectedAsLocal() {
		// Assert:
		assertAddressIsLocal("127.0.0.1", true);
	}

	@Test
	public void localIpv6AddressIsDetectedAsLocal() {
		// Assert:
		assertAddressIsLocal("0:0:0:0:0:0:0:1", true);
	}

	@Test
	public void otherAddressesAreDetectedAsRemote() {
		// Assert:
		assertAddressIsLocal("194.66.82.11", false);
		assertAddressIsLocal("127.0.0.10", false);
		assertAddressIsLocal("0:0:0:0:0:0:0:10", false);
	}

	@Test
	public void detectorCanBeCreatedWithAdditionalLocalIpAddresses() {
		// Arrange:
		final LocalHostDetector detector = new LocalHostDetector(new String[] { "194.66.82.11", "0:0:0:0:0:0:0:10" });

		// Assert:
		assertAddressIsLocal(detector, "194.66.82.11", true);
		assertAddressIsLocal(detector, "127.0.0.10", false);
		assertAddressIsLocal(detector, "0:0:0:0:0:0:0:10", true);
	}

	@Test
	public void detectorCannotBeCreatedAroundInvalidAdditionalLocalIpAddresses() {
		// Act:
		ExceptionAssert.assertThrows(
				v -> new LocalHostDetector(new String[] { "not a host" }),
				IllegalArgumentException.class);
	}

	private static void assertAddressIsLocal(final String remoteAddress, final boolean isLocal) {
		// Assert:
		assertAddressIsLocal(new LocalHostDetector(), remoteAddress, isLocal);
	}

	private static void assertAddressIsLocal(final LocalHostDetector detector, final String remoteAddress, final boolean isLocal) {
		// Arrange:
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(remoteAddress);

		// Act:
		final boolean result = detector.isLocal(request);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(isLocal));
	}
}