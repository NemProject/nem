package org.nem.core.node;

import java.util.*;
import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.ArrayUtils;

public class NodeIdentityTest {

	// region constructor

	@Test
	public void identityCanBeCreatedAroundPublicKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair(Utils.generateRandomPublicKey());

		// Act:
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		MatcherAssert.assertThat(identity.getKeyPair(), IsSame.sameInstance(keyPair));
		MatcherAssert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		MatcherAssert.assertThat(identity.isOwned(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(identity.getName(), IsNull.nullValue());
	}

	@Test
	public void identityCanBeCreatedAroundPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// Act:
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		MatcherAssert.assertThat(identity.getKeyPair(), IsSame.sameInstance(keyPair));
		MatcherAssert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		MatcherAssert.assertThat(identity.isOwned(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(identity.getName(), IsNull.nullValue());
	}

	@Test
	public void identityCanBeCreatedWithFriendlyName() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// Act:
		final NodeIdentity identity = new NodeIdentity(keyPair, "bob");

		// Assert:
		MatcherAssert.assertThat(identity.getKeyPair(), IsSame.sameInstance(keyPair));
		MatcherAssert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		MatcherAssert.assertThat(identity.isOwned(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(identity.getName(), IsEqual.equalTo("bob"));
	}

	// endregion

	// region setName

	@Test
	public void canChangeNodeIdentityFriendlyName() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair, "aaa");

		// Act:
		identity.setName("bbb");

		// Assert:
		MatcherAssert.assertThat(identity.getName(), IsEqual.equalTo("bbb"));
	}

	// endregion

	// region sign

	@Test
	public void equalIdentitiesProduceSameSignatures() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity1 = new NodeIdentity(keyPair);
		final NodeIdentity identity2 = new NodeIdentity(keyPair);

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		final Signature signature1 = identity1.sign(salt);
		final Signature signature2 = identity2.sign(salt);

		// Assert:
		MatcherAssert.assertThat(signature2, IsEqual.equalTo(signature1));
	}

	@Test
	public void equalIdentitiesWithDifferentSaltsProduceDifferentSignatures() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity1 = new NodeIdentity(keyPair);
		final NodeIdentity identity2 = new NodeIdentity(keyPair);

		// Act:
		final Signature signature1 = identity1.sign(Utils.generateRandomBytes());
		final Signature signature2 = identity2.sign(Utils.generateRandomBytes());

		// Assert:
		MatcherAssert.assertThat(signature2, IsNot.not(IsEqual.equalTo(signature1)));
	}

	@Test
	public void differentIdentitiesProduceDifferentSignatures() {
		// Arrange:
		final NodeIdentity identity1 = new NodeIdentity(new KeyPair());
		final NodeIdentity identity2 = new NodeIdentity(new KeyPair());

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		final Signature signature1 = identity1.sign(salt);
		final Signature signature2 = identity2.sign(salt);

		// Assert:
		MatcherAssert.assertThat(signature2, IsNot.not(IsEqual.equalTo(signature1)));
	}

	@Test(expected = CryptoException.class)
	public void identityCannotSignSaltWithoutPrivateKey() {
		// Arrange:
		final NodeIdentity identity = new NodeIdentity(new KeyPair(Utils.generateRandomPublicKey()));

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		identity.sign(salt);
	}

	// endregion

	// region verify

	@Test
	public void signatureCanBeVerifiedByEqualIdentityWithoutPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity1 = new NodeIdentity(keyPair);
		final NodeIdentity identity2 = new NodeIdentity(new KeyPair(keyPair.getPublicKey()));

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		final Signature signature = identity1.sign(salt);
		final boolean isVerified = identity2.verify(salt, signature);

		// Assert:
		MatcherAssert.assertThat(isVerified, IsEqual.equalTo(true));
	}

	@Test
	public void signatureCannotBeVerifiedBySameIdentityWithDifferentSalt() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity1 = new NodeIdentity(keyPair);
		final NodeIdentity identity2 = new NodeIdentity(keyPair);

		// Act:
		final Signature signature = identity1.sign(Utils.generateRandomBytes());
		final boolean isVerified = identity2.verify(Utils.generateRandomBytes(), signature);

		// Assert:
		MatcherAssert.assertThat(isVerified, IsEqual.equalTo(false));
	}

	@Test
	public void signatureCannotBeVerifiedByDifferentIdentity() {
		// Arrange:
		final NodeIdentity identity1 = new NodeIdentity(new KeyPair());
		final NodeIdentity identity2 = new NodeIdentity(new KeyPair());

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		final Signature signature = identity1.sign(salt);
		final boolean isVerified = identity2.verify(salt, signature);

		// Assert:
		MatcherAssert.assertThat(isVerified, IsEqual.equalTo(false));
	}

	@Test
	public void signatureCannotBeVerifiedWithoutChallengePrefix() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);

		final byte[] payload = "alice is bad".getBytes();
		final byte[] challengePrefix = "nem trust challenge:".getBytes();
		final byte[] publicKey = keyPair.getPublicKey().getRaw();
		final byte[] payloadWithPrefix = ArrayUtils.concat(challengePrefix, publicKey, payload);

		final Signer signer = new Signer(keyPair);
		final Signature signedDataWithPrefix = signer.sign(payloadWithPrefix);
		final Signature signedDataWithoutPrefix = signer.sign(payload);

		// Assert:
		MatcherAssert.assertThat(identity.verify(payload, signedDataWithPrefix), IsEqual.equalTo(true));
		MatcherAssert.assertThat(identity.verify(payload, signedDataWithoutPrefix), IsEqual.equalTo(false));
	}

	// endregion

	// region serialization

	@Test
	public void identityWithPublicKeyCanBeRoundTripped() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();

		// Act:
		final NodeIdentity identity = createRoundTrippedIdentity(new NodeIdentity(new KeyPair(publicKey), "alice"));

		// Assert:
		MatcherAssert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(publicKey));
		MatcherAssert.assertThat(identity.getKeyPair().getPrivateKey(), IsNull.nullValue());
		MatcherAssert.assertThat(identity.isOwned(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(identity.getName(), IsEqual.equalTo("alice"));
	}

	@Test
	public void identityWithPrivateKeyCanBeRoundTrippedWithoutPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// ActL
		final NodeIdentity identity = createRoundTrippedIdentity(new NodeIdentity(keyPair, "bob"));

		// Assert:
		MatcherAssert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		MatcherAssert.assertThat(identity.getKeyPair().getPrivateKey(), IsNull.nullValue());
		MatcherAssert.assertThat(identity.isOwned(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(identity.getName(), IsEqual.equalTo("bob"));
	}

	@Test
	public void serializerPayloadDoesNotContainPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		identity.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();

		// Assert:
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(jsonObject.containsKey("public-key"), IsEqual.equalTo(true));
		MatcherAssert.assertThat(jsonObject.containsKey("name"), IsEqual.equalTo(true));
	}

	@Test
	public void jsonContainingPrivateKeyCanBeDeserialized() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final JsonSerializer serializer = new JsonSerializer(true);
		serializer.writeBigInteger("private-key", keyPair.getPrivateKey().getRaw());
		serializer.writeString("name", "trudy");

		// Act:
		final Deserializer deserializer = new JsonDeserializer(serializer.getObject(), null);
		final NodeIdentity identity = NodeIdentity.deserializeWithPrivateKey(deserializer);

		// Assert:
		MatcherAssert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		MatcherAssert.assertThat(identity.getKeyPair().getPrivateKey(), IsEqual.equalTo(keyPair.getPrivateKey()));
		MatcherAssert.assertThat(identity.isOwned(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(identity.getName(), IsEqual.equalTo("trudy"));
	}

	private static NodeIdentity createRoundTrippedIdentity(final NodeIdentity originalIdentity) {
		return NodeIdentity.deserializeWithPublicKey(Utils.roundtripSerializableEntity(originalIdentity, null));
	}

	// endregion

	// region equals / hashCode

	@SuppressWarnings("serial")
	private static final Map<String, NodeIdentity> DESC_TO_IDENTITY_MAP = new HashMap<String, NodeIdentity>() {
		{
			final KeyPair keyPair = new KeyPair();
			this.put("DEFAULT", new NodeIdentity(keyPair));
			this.put("SAME_KEY_PAIR", new NodeIdentity(keyPair));
			this.put("SAME_KEY_PAIR_DIFF_NAME", new NodeIdentity(keyPair, "zzz"));
			this.put("SAME_PUB_KEY", new NodeIdentity(new KeyPair(keyPair.getPublicKey())));
			this.put("DIFF_KEY_PAIR", new NodeIdentity(new KeyPair()));
			this.put("DIFF_PUB_KEY", new NodeIdentity(new KeyPair(Utils.generateRandomPublicKey())));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeIdentity identity = DESC_TO_IDENTITY_MAP.get("DEFAULT");

		// Assert:
		for (final Map.Entry<String, NodeIdentity> entry : DESC_TO_IDENTITY_MAP.entrySet()) {
			if (entry.getKey().equals("DEFAULT") || entry.getKey().startsWith("SAME_")) {
				MatcherAssert.assertThat(entry.getValue(), IsEqual.equalTo(identity));
			} else {
				MatcherAssert.assertThat(entry.getValue(), IsNot.not(IsEqual.equalTo(identity)));
			}
		}

		MatcherAssert.assertThat(identity, IsNot.not(IsEqual.equalTo(identity.getKeyPair())));
		MatcherAssert.assertThat(identity, IsNot.not(IsEqual.equalTo(null)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = DESC_TO_IDENTITY_MAP.get("DEFAULT").hashCode();

		// Assert:
		for (final Map.Entry<String, NodeIdentity> entry : DESC_TO_IDENTITY_MAP.entrySet()) {
			if (entry.getKey().equals("DEFAULT") || entry.getKey().startsWith("SAME_")) {
				MatcherAssert.assertThat(entry.getValue().hashCode(), IsEqual.equalTo(hashCode));
			} else {
				MatcherAssert.assertThat(entry.getValue().hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
			}
		}
	}

	// endregion

	// region toString

	private static final PublicKey PUBLIC_KEY_FOR_TO_STRING_TESTS = PublicKey
			.fromHexString("509079d0252d8e24aef0402403618515717970345b1192aa8c3522c6292aa648");

	@Test
	public void toStringReturnsAppropriateRepresentationWhenNameIsPresent() {
		// Arrange:
		final KeyPair keyPair = new KeyPair(PUBLIC_KEY_FOR_TO_STRING_TESTS);
		final NodeIdentity identity = new NodeIdentity(keyPair, "bob");

		// Assert:
		MatcherAssert.assertThat(identity.toString(), IsEqual.equalTo("bob <TAXYZDGH33FMGBDAY3LYBHVVDSNPTYKVRMQSJ7RY>"));
	}

	@Test
	public void toStringReturnsAppropriateRepresentationWhenNameIsNotPresent() {
		// Arrange:
		final KeyPair keyPair = new KeyPair(PUBLIC_KEY_FOR_TO_STRING_TESTS);
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		MatcherAssert.assertThat(identity.toString(), IsEqual.equalTo("<TAXYZDGH33FMGBDAY3LYBHVVDSNPTYKVRMQSJ7RY>"));
	}

	// endregion
}
