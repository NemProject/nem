package org.nem.nis.controller.interceptors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.ExceptionAssert;

import javax.servlet.http.HttpServletRequest;

public class LocalHostDetectorTest {

	// region default local address detection

	@Test
	public void localhostIsDetectedAsLocal() {
		// Assert:
		assertAddressIsLocal("localhost", true);
	}

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

	// endregion

	// region custom local address support

	@Test
	public void detectorCanBeCreatedWithHostNames() {
		// Arrange:
		final LocalHostDetector detector = new LocalHostDetector(new String[]{
				"bob.nem.ninja"
		});

		// Assert:
		assertAddressIsLocal(detector, "bob.nem.ninja", true);
		assertAddressIsLocal(detector, "go.nem.ninja", false);
	}

	@Test
	public void detectorCanBeCreatedWithAdditionalLocalIpAddresses() {
		// Arrange:
		final LocalHostDetector detector = new LocalHostDetector(new String[]{
				"194.66.82.11", "0:0:0:0:0:0:0:10"
		});

		// Assert:
		assertAddressIsLocal(detector, "194.66.82.11", true);
		assertAddressIsLocal(detector, "127.0.0.10", false);
		assertAddressIsLocal(detector, "0:0:0:0:0:0:0:10", true);
	}

	@Test
	public void detectorCanBeCreatedWithAdditionalLocalIpv4WildcardAddresses() {
		// Arrange:
		final LocalHostDetector detector = new LocalHostDetector(new String[]{
				"194.*.82.11", "*.12.82.*"
		});

		// Assert:
		assertAddressIsLocal(detector, "194.66.82.11", true);
		assertAddressIsLocal(detector, "194.77.82.11", true);
		assertAddressIsLocal(detector, "194.77.82.12", false);

		assertAddressIsLocal(detector, "127.12.82.11", true);
		assertAddressIsLocal(detector, "101.12.82.88", true);
		assertAddressIsLocal(detector, "101.12.81.88", false);

		assertAddressIsLocal(detector, "194:66:82:11:0:0:0:10", false);
	}

	@Test
	public void detectorCanBeCreatedWithAdditionalLocalIpv6WildcardAddresses() {
		// Arrange:
		final LocalHostDetector detector = new LocalHostDetector(new String[]{
				"*:0:0:0:0:0:0:10", "0:0:0:0:0:*:*:10"
		});

		// Assert:
		assertAddressIsLocal(detector, "11:0:0:0:0:0:0:10", true);
		assertAddressIsLocal(detector, "92:0:0:0:0:0:0:10", true);
		assertAddressIsLocal(detector, "92:0:0:0:0:0:0:11", false);

		assertAddressIsLocal(detector, "0:0:0:0:0:77:77:10", true);
		assertAddressIsLocal(detector, "0:0:0:0:0:77:84:10", true);
		assertAddressIsLocal(detector, "0:0:0:0:1:77:84:10", false);

		assertAddressIsLocal(detector, "0.0.0.0", false);
	}

	@Test
	public void detectorCannotBeCreatedAroundInvalidAdditionalLocalIpAddresses() {
		// Assert:
		assertInvalidAddress("not a host");
		assertInvalidAddress("127.*.0.0.0.0.0.0"); // invalid ipv4 wildcard
		assertInvalidAddress("11:0:*:0"); // invalid ipv6 wildcard
		assertInvalidAddress("*|12"); // invalid wildcard
		assertInvalidAddress("*"); // invalid wildcard
	}

	// endregion

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
		MatcherAssert.assertThat(result, IsEqual.equalTo(isLocal));
	}

	private static void assertInvalidAddress(final String address) {
		// Act:
		ExceptionAssert.assertThrows(v -> new LocalHostDetector(new String[]{
				address
		}), IllegalArgumentException.class);
	}
}
