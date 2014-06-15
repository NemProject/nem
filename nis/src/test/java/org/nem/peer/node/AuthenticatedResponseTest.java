package org.nem.peer.node;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;

public class AuthenticatedResponseTest {

	@Test
	public void responseCanBeCreated() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);
		final NodeIdentity identity = new NodeIdentity(new KeyPair());

		// Act:
		final AuthenticatedResponse<?> response = new AuthenticatedResponse<>(entity, identity, challenge);

		// Assert:
		Assert.assertThat(response.getSignature(), IsEqual.equalTo(identity.sign(challenge.getRaw())));
		Assert.assertThat(response.getEntity(identity, challenge), IsEqual.equalTo(entity));
	}

	@Test
	public void responseCanBeRoundTripped() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);
		final NodeIdentity identity = new NodeIdentity(new KeyPair());

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new AuthenticatedResponse<>(entity, identity, challenge),
				null);
		final AuthenticatedResponse<?> response = new AuthenticatedResponse<>(deserializer, obj -> new MockSerializableEntity(obj));

		// Assert:
		Assert.assertThat(response.getSignature(), IsEqual.equalTo(identity.sign(challenge.getRaw())));
		Assert.assertThat(response.getEntity(identity, challenge), IsEqual.equalTo(entity));
	}

	@Test(expected = ImpersonatingPeerException.class)
	public void responseGetEntityFailsIfPeerIsImpersonating() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);
		final NodeIdentity identity1 = new NodeIdentity(new KeyPair());
		final NodeIdentity identity2 = new NodeIdentity(new KeyPair());

		// Act:
		final AuthenticatedResponse<?> response = new AuthenticatedResponse<>(entity, identity1, challenge);

		// Assert:
		Assert.assertThat(response.getEntity(identity2, challenge), IsEqual.equalTo(entity));
	}
}