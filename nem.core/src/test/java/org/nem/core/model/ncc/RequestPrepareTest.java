package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.Utils;

public class RequestPrepareTest {

	@Test
	public void canCreateRequest() {
		// Arrange:
		final RequestPrepare request = new RequestPrepare(new byte[] { 1, 3, 4, 7, 7, 8, 9 });

		// Assert:
		Assert.assertThat(request.getData(), IsEqual.equalTo(new byte[] { 1, 3, 4, 7, 7, 8, 9 }));
	}

	@Test
	public void canRoundTripRequest() {
		// Arrange:
		final RequestPrepare originalRequest = new RequestPrepare(new byte[] { 1, 3, 4, 7, 7, 8, 9 });

		// Act:
		final RequestPrepare request = createRoundTrippedRequest(originalRequest);

		// Assert:
		Assert.assertThat(request.getData(), IsEqual.equalTo(new byte[] { 1, 3, 4, 7, 7, 8, 9 }));
	}

	private static RequestPrepare createRoundTrippedRequest(final RequestPrepare originalRequest) {
		// Act:
		return new RequestPrepare(Utils.roundtripSerializableEntity(originalRequest, null));
	}
}
