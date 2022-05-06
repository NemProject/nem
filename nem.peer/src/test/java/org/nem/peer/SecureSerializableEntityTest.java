package org.nem.peer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.HashUtils;
import org.nem.core.node.NodeIdentity;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.peer.node.ImpersonatingPeerException;

public class SecureSerializableEntityTest {

	@Test
	public void secureEntityCanBeCreated() {
		// Arrange:
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);
		final NodeIdentity identity = new NodeIdentity(new KeyPair());

		// Act:
		final SecureSerializableEntity<?> secureEntity = new SecureSerializableEntity<>(entity, identity);

		// Assert:
		MatcherAssert.assertThat(secureEntity.getSignature(), IsEqual.equalTo(identity.sign(HashUtils.calculateHash(entity).getRaw())));
		MatcherAssert.assertThat(secureEntity.getEntity(), IsEqual.equalTo(entity));
		MatcherAssert.assertThat(secureEntity.getIdentity(), IsEqual.equalTo(identity));
	}

	@Test
	public void secureEntityCanBeRoundTripped() {
		// Arrange:
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);
		final NodeIdentity identity = new NodeIdentity(new KeyPair());

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(new SecureSerializableEntity<>(entity, identity), null);
		final SecureSerializableEntity<?> secureEntity = new SecureSerializableEntity<>(deserializer, MockSerializableEntity::new);

		// Assert:
		MatcherAssert.assertThat(secureEntity.getSignature(), IsEqual.equalTo(identity.sign(HashUtils.calculateHash(entity).getRaw())));
		MatcherAssert.assertThat(secureEntity.getEntity(), IsEqual.equalTo(entity));
		MatcherAssert.assertThat(secureEntity.getIdentity(), IsEqual.equalTo(identity));
	}

	@Test(expected = ImpersonatingPeerException.class)
	public void secureEntityGetEntityFailsIfPeerIsImpersonating() {
		// Arrange:
		final MockSerializableEntity entity = new MockSerializableEntity(1, "blah", 44);
		final NodeIdentity identity1 = new NodeIdentity(new KeyPair());
		final NodeIdentity identity2 = new NodeIdentity(new KeyPair());
		final SecureSerializableEntity<?> secureEntity = new SecureSerializableEntity<>(entity, identity1);

		final JsonSerializer serializer = new JsonSerializer();
		secureEntity.serialize(serializer);
		serializer.getObject().put("identity", JsonSerializer.serializeToJson(identity2));
		final SecureSerializableEntity<?> insecureEntity = new SecureSerializableEntity<>(
				new JsonDeserializer(serializer.getObject(), null), MockSerializableEntity::new);

		// Act:
		insecureEntity.getEntity();
	}
}
