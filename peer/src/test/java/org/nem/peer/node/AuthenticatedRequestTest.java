package org.nem.peer.node;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;

public class AuthenticatedRequestTest {

	@Test
	public void requestCanBeCreated() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);

		// Act:
		final AuthenticatedRequest<?> request = new AuthenticatedRequest<>(entity, challenge);

		// Assert:
		MatcherAssert.assertThat(request.getChallenge(), IsEqual.equalTo(challenge));
		MatcherAssert.assertThat(request.getEntity(), IsEqual.equalTo(entity));
	}

	@Test
	public void requestCanBeRoundTripped() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(new AuthenticatedRequest<>(entity, challenge), null);
		final AuthenticatedRequest<?> request = new AuthenticatedRequest<>(deserializer, MockSerializableEntity::new);

		// Assert:
		MatcherAssert.assertThat(request.getChallenge(), IsEqual.equalTo(challenge));
		MatcherAssert.assertThat(request.getEntity(), IsEqual.equalTo(entity));
	}
}
