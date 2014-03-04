package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.test.Utils;

public class RequestAnnounceTest {
	@Test
	public void requestAnnounceCanBeRoundTripped() {
		// Arrange:
		byte[] data = { 1,2,3,4,5,6,7,8,9 };
		byte[] signature = { 9,8,7,6,5,4,3,2,1 };
		final RequestAnnounce originalRequestAnnounce = new RequestAnnounce(data, signature);

		// Act:
		final RequestAnnounce requestAnnounce = createRoundTrippedRequestAnnounce(originalRequestAnnounce);

		// Assert:
		Assert.assertThat(requestAnnounce.getData(), IsEqual.equalTo(data));
		Assert.assertThat(requestAnnounce.getSignature(), IsEqual.equalTo(signature));
	}

	private static RequestAnnounce createRoundTrippedRequestAnnounce(final RequestAnnounce originalMessage) {
		// Act:
		return new RequestAnnounce(Utils.roundtripSerializableEntity(originalMessage, null));
	}
}
