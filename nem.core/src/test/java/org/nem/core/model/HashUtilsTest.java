package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class HashUtilsTest {

	//region calculateHash

	@Test
	public void identicalEntitiesHaveSameHash() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity1 = new MockVerifiableEntity(signer, 7);
		final MockVerifiableEntity entity2 = new MockVerifiableEntity(signer, 7);

		// Act:
		final Hash hash1 = HashUtils.calculateHash(entity1);
		final Hash hash2 = HashUtils.calculateHash(entity2);

		// Assert:
		Assert.assertThat(hash1, IsEqual.equalTo(hash2));
	}

	@Test
	public void differentEntitiesHaveDifferentHashes() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity1 = new MockVerifiableEntity(signer, 7);
		final MockVerifiableEntity entity2 = new MockVerifiableEntity(signer, 8);

		// Act:
		final Hash hash1 = HashUtils.calculateHash(entity1);
		final Hash hash2 = HashUtils.calculateHash(entity2);

		// Assert:
		Assert.assertThat(hash1, IsNot.not(IsEqual.equalTo(hash2)));
	}

	@Test
	public void signatureDoesNotChangeEntity() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity1 = new MockVerifiableEntity(signer, 7);
		final MockVerifiableEntity entity2 = new MockVerifiableEntity(signer, 7);
		entity2.sign();

		// Act:
		final Hash hash1 = HashUtils.calculateHash(entity1);
		final Hash hash2 = HashUtils.calculateHash(entity2);

		// Assert:
		Assert.assertThat(hash1, IsEqual.equalTo(hash2));
	}

	@Test
	public void changingPayloadChangesHash() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);

		final Hash hash1 = HashUtils.calculateHash(entity);

		// Act:
		entity.setCustomField(6);
		final Hash hash2 = HashUtils.calculateHash(entity);

		// Assert:
		Assert.assertThat(hash1, IsNot.not(IsEqual.equalTo(hash2)));
	}

	//endregion

	//region nextHash

	@Test
	public void nextHashProducesHashEquivalentToConcatenatingTheInputs() {
		// Arrange:
		final Hash inputHash = Utils.generateRandomHash();
		final PublicKey inputKey = Utils.generateRandomPublicKey();

		// Act:
		final Hash hash1 = HashUtils.nextHash(inputHash, inputKey);
		final Hash hash2 = HashUtils.calculateHash(new HashPublicKeyPair(inputHash, inputKey));

		// Assert:
		Assert.assertThat(hash1, IsNot.not(IsEqual.equalTo(hash2)));
	}

	//endregion

	private static class HashPublicKeyPair implements SerializableEntity {

		private final Hash hash;
		private final PublicKey key;

		public HashPublicKeyPair(final Hash hash, final PublicKey key) {
			this.hash = hash;
			this.key = key;
		}

		@Override
		public void serialize(final Serializer serializer) {
			serializer.writeBytes("hash", this.hash.getRaw());
			serializer.writeBytes("key", this.key.getRaw());
		}
	}
}
