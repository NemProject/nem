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
		final byte[] data = Utils.generateRandomBytes();
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);
		final NodeIdentity identity = new NodeIdentity(new KeyPair());

		// Act:
		final AuthenticatedResponse<?> response = new AuthenticatedResponse<>(entity, identity, data);

		// Assert:
		Assert.assertThat(response.getSignature(), IsEqual.equalTo(identity.sign(data)));
		Assert.assertThat(response.getEntity(identity, data), IsEqual.equalTo(entity));
	}

	@Test
	public void responseCanBeRoundTripped() {
		// Arrange:
		final byte[] data = Utils.generateRandomBytes();
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);
		final NodeIdentity identity = new NodeIdentity(new KeyPair());

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new AuthenticatedResponse<>(entity, identity, data),
				null);
		final AuthenticatedResponse<?> response = new AuthenticatedResponse<>(deserializer, MockSerializableEntity::new);

		// Assert:
		Assert.assertThat(response.getSignature(), IsEqual.equalTo(identity.sign(data)));
		Assert.assertThat(response.getEntity(identity, data), IsEqual.equalTo(entity));
	}

	@Test(expected = ImpersonatingPeerException.class)
	public void responseGetEntityFailsIfPeerIsImpersonating() {
		// Arrange:
		final byte[] data = Utils.generateRandomBytes();
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);
		final NodeIdentity identity1 = new NodeIdentity(new KeyPair());
		final NodeIdentity identity2 = new NodeIdentity(new KeyPair());

		// Act:
		final AuthenticatedResponse<?> response = new AuthenticatedResponse<>(entity, identity1, data);

		// Assert:
		Assert.assertThat(response.getEntity(identity2, data), IsEqual.equalTo(entity));
	}
}