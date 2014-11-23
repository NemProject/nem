package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.Utils;

public class RequestAnnounceTest {

	@Test
	public void canCreateRequest() {
		// Arrange:
		final RequestAnnounce request = new RequestAnnounce(
				new byte[] { 1, 3, 4, 7, 7, 8, 9 },
				new byte[] { 4, 5, 7, 2, 3 });

		// Assert:
		Assert.assertThat(request.getData(), IsEqual.equalTo(new byte[] { 1, 3, 4, 7, 7, 8, 9 }));
		Assert.assertThat(request.getSignature(), IsEqual.equalTo(new byte[] { 4, 5, 7, 2, 3 }));
	}

	@Test
	public void canRoundTripRequest() {
		// Arrange:
		final RequestAnnounce originalRequest = new RequestAnnounce(
				new byte[] { 1, 3, 4, 7, 7, 8, 9 },
				new byte[] { 4, 5, 7, 2, 3 });

		// Act:
		final RequestAnnounce request = createRoundTrippedRequest(originalRequest);

		// Assert:
		Assert.assertThat(request.getData(), IsEqual.equalTo(new byte[] { 1, 3, 4, 7, 7, 8, 9 }));
		Assert.assertThat(request.getSignature(), IsEqual.equalTo(new byte[] { 4, 5, 7, 2, 3 }));
	}

	private static RequestAnnounce createRoundTrippedRequest(final RequestAnnounce originalRequest) {
		// Act:
		return new RequestAnnounce(Utils.roundtripSerializableEntity(originalRequest, null));
	}
}