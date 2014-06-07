package org.nem.peer.node;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.test.Utils;

public class NodeIdentityTest {

	//region constructor

	@Test
	public void identityCanBeCreatedAroundPublicKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair(Utils.generateRandomPublicKey());

		// Act:
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		Assert.assertThat(identity.getKeyPair(), IsSame.sameInstance(keyPair));
		Assert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		Assert.assertThat(identity.isOwned(), IsEqual.equalTo(false));
	}

	@Test
	public void identityCanBeCreatedAroundPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// Act:
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		Assert.assertThat(identity.getKeyPair(), IsSame.sameInstance(keyPair));
		Assert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		Assert.assertThat(identity.isOwned(), IsEqual.equalTo(true));
	}

	//endregion

	//region

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
		Assert.assertThat(signature2, IsEqual.equalTo(signature1));
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
		Assert.assertThat(signature2, IsNot.not(IsEqual.equalTo(signature1)));
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
		Assert.assertThat(signature2, IsNot.not(IsEqual.equalTo(signature1)));
	}

	@Test(expected = CryptoException.class)
	public void identityCannotSignSaltWithoutPrivateKey() {
		// Arrange:
		final NodeIdentity identity = new NodeIdentity(new KeyPair(Utils.generateRandomPublicKey()));

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		identity.sign(salt);
	}

	//endregion

	//region verify

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
		Assert.assertThat(isVerified, IsEqual.equalTo(true));
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
		Assert.assertThat(isVerified, IsEqual.equalTo(false));
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
		Assert.assertThat(isVerified, IsEqual.equalTo(false));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		Assert.assertThat(new NodeIdentity(keyPair), IsEqual.equalTo(identity));
		Assert.assertThat(new NodeIdentity(new KeyPair(keyPair.getPublicKey())), IsEqual.equalTo(identity));
		Assert.assertThat(new NodeIdentity(new KeyPair()), IsNot.not(IsEqual.equalTo(identity)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(identity)));
		Assert.assertThat(keyPair, IsNot.not(IsEqual.equalTo((Object)identity)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);
		int hashCode = identity.hashCode();

		// Assert:
		Assert.assertThat(new NodeIdentity(keyPair).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeIdentity(new KeyPair(keyPair.getPublicKey())).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeIdentity(new KeyPair()).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsAddressStringRepresentation() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		Assert.assertThat(identity.toString(), IsEqual.equalTo(identity.getAddress().toString()));
	}

	//endregion
}