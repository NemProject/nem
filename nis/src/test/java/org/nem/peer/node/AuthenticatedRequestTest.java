package org.nem.peer.node;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;

public class AuthenticatedRequestTest {

	@Test
	public void requestCanBeCreated() {
		// Arrange:
		final byte[] data = Utils.generateRandomBytes();
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);

		// Act:
		final AuthenticatedRequest<?> request = new AuthenticatedRequest<>(entity, data);

		// Assert:
		Assert.assertThat(request.getData(), IsEqual.equalTo(data));
		Assert.assertThat(request.getEntity(), IsEqual.equalTo(entity));
	}

	@Test
	public void requestCanBeRoundTripped() {
		// Arrange:
		final byte[] data = Utils.generateRandomBytes();
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new AuthenticatedRequest<>(entity, data),
				null);
		final AuthenticatedRequest<?> request = new AuthenticatedRequest<>(deserializer, MockSerializableEntity::new);

		// Assert:
		Assert.assertThat(request.getData(), IsEqual.equalTo(data));
		Assert.assertThat(request.getEntity(), IsEqual.equalTo(entity));
	}
}