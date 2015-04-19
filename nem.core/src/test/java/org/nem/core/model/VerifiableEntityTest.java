package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class VerifiableEntityTest {

	//region Constructor

	@Test
	public void ctorCanCreateEntityForAccountWithSignerPrivateKey() {
		// Arrange:
		final KeyPair publicPrivateKeyPair = new KeyPair();
		final Account signer = new Account(publicPrivateKeyPair);

		// Act:
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 6);

		// Assert:
		Assert.assertThat(entity.getType(), IsEqual.equalTo(MockVerifiableEntity.TYPE));
		Assert.assertThat(entity.getVersion(), IsEqual.equalTo(0x98000000 | MockVerifiableEntity.VERSION));
		Assert.assertThat(entity.getTimeStamp(), IsEqual.equalTo(MockVerifiableEntity.TIMESTAMP));
		Assert.assertThat(entity.getCustomField(), IsEqual.equalTo(6));
		Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(entity.getSignature(), IsNull.nullValue());
	}

	@Test
	public void ctorCanCreateEntityForAccountWithoutSignerPrivateKey() {
		// Arrange:
		final KeyPair publicPrivateKeyPair = new KeyPair();
		final KeyPair publicOnlyKeyPair = new KeyPair(publicPrivateKeyPair.getPublicKey());

		// Act:
		new MockVerifiableEntity(new Account(publicOnlyKeyPair));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorCannotCreateEntityForAccountWithoutSignerKeyPair() {
		// Arrange:
		final Address address = Address.fromEncoded("Alpha");

		// Act:
		new MockVerifiableEntity(new Account(address));
	}

	@Test
	public void cannotCreateEntityWithVersionUsingReservedNetworkBytes() {
		// Act:
		ExceptionAssert.assertThrows(v -> new MockVerifiableEntityWithCustomVersion(0x01000000), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new MockVerifiableEntityWithCustomVersion(0x80000000), IllegalArgumentException.class);
	}

	//endregion

	//region Serialization

	@Test
	public void verifiableEntityCanBeRoundTripped() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
		final MockVerifiableEntity originalEntity = new MockVerifiableEntity(signer, 7);
		final MockVerifiableEntity entity = createRoundTrippedEntity(originalEntity, signerPublicKeyOnly);

		// Assert:
		Assert.assertThat(entity.getType(), IsEqual.equalTo(MockVerifiableEntity.TYPE));
		Assert.assertThat(entity.getVersion(), IsEqual.equalTo(0x98000000 | MockVerifiableEntity.VERSION));
		Assert.assertThat(entity.getTimeStamp(), IsEqual.equalTo(MockVerifiableEntity.TIMESTAMP));
		Assert.assertThat(entity.getCustomField(), IsEqual.equalTo(7));
		Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signerPublicKeyOnly));
		Assert.assertThat(entity.getSignature(), IsNull.notNullValue());
	}

	@Test
	public void nonVerifiableEntityCanBeRoundTripped() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
		final MockVerifiableEntity originalEntity = new MockVerifiableEntity(signer, 7);
		final MockVerifiableEntity entity = createNonVerifiableRoundTrippedEntity(originalEntity, signerPublicKeyOnly);

		// Assert:
		Assert.assertThat(entity.getType(), IsEqual.equalTo(MockVerifiableEntity.TYPE));
		Assert.assertThat(entity.getVersion(), IsEqual.equalTo(0x98000000 | MockVerifiableEntity.VERSION));
		Assert.assertThat(entity.getTimeStamp(), IsEqual.equalTo(MockVerifiableEntity.TIMESTAMP));
		Assert.assertThat(entity.getCustomField(), IsEqual.equalTo(7));
		Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signerPublicKeyOnly));
		Assert.assertThat(entity.getSignature(), IsNull.nullValue());
	}

	@Test(expected = SerializationException.class)
	public void verifiableSerializationRequiresSignature() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer);

		// Act:
		entity.serialize(new JsonSerializer());
	}

	@Test
	public void nonVerifiableSerializationDoesNotRequireSignature() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer);

		// Act:
		entity.asNonVerifiable().serialize(new JsonSerializer());
	}

	@Test
	public void verifiableSerializationIncludesSignature() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer);
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		entity.sign();
		entity.serialize(serializer);
		final JSONObject object = serializer.getObject();

		// Assert:
		Assert.assertThat(object.containsKey("signature"), IsEqual.equalTo(true));
	}

	@Test
	public void nonVerifiableSerializationExcludesSignature() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer);
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		entity.sign();
		entity.asNonVerifiable().serialize(serializer);
		final JSONObject object = serializer.getObject();

		// Assert:
		Assert.assertThat(object.containsKey("signature"), IsEqual.equalTo(false));
	}

	//endregion

	//region Sign / Verify

	@Test
	public void signCreatesValidSignature() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer);

		// Act:
		entity.sign();

		// Assert:
		Assert.assertThat(entity.getSignature(), IsNull.notNullValue());
		Assert.assertThat(entity.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void signByCreatesValidSignature() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
		final MockVerifiableEntity entity = new MockVerifiableEntity(signerPublicKeyOnly);

		// Act:
		entity.signBy(signer);

		// Assert:
		Assert.assertThat(entity.getSignature(), IsNull.notNullValue());
		Assert.assertThat(entity.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void changingFieldInvalidatesSignature() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntityWithNonVerifiableData entity = new MockVerifiableEntityWithNonVerifiableData(signer, 7, 11, 6);

		// Act:
		entity.sign();
		entity.setVerifiableField1(5);

		// Assert:
		Assert.assertThat(entity.getSignature(), IsNull.notNullValue());
		Assert.assertThat(entity.verify(), IsEqual.equalTo(false));
	}

	@Test
	public void changingNonVerifiableFieldDoesNotInvalidateSignature() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntityWithNonVerifiableData entity = new MockVerifiableEntityWithNonVerifiableData(signer, 7, 11, 6);

		// Act:
		entity.sign();
		entity.setNonVerifiableField1(5);

		// Assert:
		Assert.assertThat(entity.getSignature(), IsNull.notNullValue());
		Assert.assertThat(entity.verify(), IsEqual.equalTo(true));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotSignWithoutPrivateKey() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
		final MockVerifiableEntity entity = createRoundTrippedEntity(signer, 7, signerPublicKeyOnly);

		// Assert:
		entity.sign();
	}

	@Test(expected = CryptoException.class)
	public void cannotVerifyWithoutSignature() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);

		// Act:
		entity.verify();
	}

	@Test
	public void verifiableRoundTrippedEntityCanBeVerified() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
		final MockVerifiableEntity entity = createRoundTrippedEntity(signer, 7, signerPublicKeyOnly);

		// Assert:
		Assert.assertThat(entity.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void verifiableRoundTrippedEntityCanBeVerifiedWhenSignerAccountIsUnknown() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final AccountLookup accountLookup = new MockAccountLookup(MockAccountLookup.UnknownAccountBehavior.REAL_ACCOUNT);
		final MockVerifiableEntity entity = createRoundTrippedEntity(signer, 7, accountLookup);

		// Assert:
		Assert.assertThat(entity.verify(), IsEqual.equalTo(true));
	}

	@Test(expected = CryptoException.class)
	public void nonVerifiableRoundTrippedEntityCannotBeVerified() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
		final MockVerifiableEntity originalEntity = new MockVerifiableEntity(signer);
		final MockVerifiableEntity entity = createNonVerifiableRoundTrippedEntity(originalEntity, signerPublicKeyOnly);

		// Assert:
		entity.verify();
	}

	//endregion

	//region External Signature

	@Test
	public void signatureCanBeSetExternally() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);
		final Signature signature = new Signature(Utils.generateRandomBytes(64));

		// Act:
		entity.setSignature(signature);

		// Assert:
		Assert.assertThat(entity.getSignature(), IsEqual.equalTo(signature));
	}

	@Test
	public void nonMatchingExternalSignatureCannotBeVerified() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);

		// Act:
		entity.setSignature(new Signature(Utils.generateRandomBytes(64)));

		// Assert:
		Assert.assertThat(entity.verify(), IsEqual.equalTo(false));
	}

	@Test
	public void matchingExternalSignatureCanBeVerified() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntity entity1 = new MockVerifiableEntity(signer, 7);
		final MockVerifiableEntity entity2 = new MockVerifiableEntity(signer, 7);

		// Act:
		entity1.sign();
		entity2.setSignature(entity1.getSignature());

		// Assert:
		Assert.assertThat(entity2.verify(), IsEqual.equalTo(true));
	}

	//endregion

	//region non-verifiable data

	@Test
	public void verifiableEntityWithNonVerifiableCanBeRoundTripped() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntityWithNonVerifiableData originalEntity = create(signer, 7, 9, 4);
		final MockVerifiableEntityWithNonVerifiableData entity = new MockVerifiableEntityWithNonVerifiableData(
				VerifiableEntity.DeserializationOptions.VERIFIABLE,
				Utils.roundtripVerifiableEntity(originalEntity, new MockAccountLookup()));

		// Assert:
		assertVerifiableEntityProperties(entity, signer);
		Assert.assertThat(entity.getVerifiableField1(), IsEqual.equalTo(7));
		Assert.assertThat(entity.getVerifiableField2(), IsEqual.equalTo(9));
		Assert.assertThat(entity.getNonVerifiableField1(), IsEqual.equalTo(4));
		Assert.assertThat(entity.getSignature(), IsNull.notNullValue());
	}

	@Test
	public void nonVerifiableEntityWithNonVerifiableCanBeRoundTrippedWithoutNonVerifiableData() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockVerifiableEntityWithNonVerifiableData originalEntity = create(signer, 7, 9, 4);
		final MockVerifiableEntityWithNonVerifiableData entity = new MockVerifiableEntityWithNonVerifiableData(
				VerifiableEntity.DeserializationOptions.NON_VERIFIABLE,
				Utils.roundtripSerializableEntity(originalEntity.asNonVerifiable(), new MockAccountLookup()));

		// Assert:
		assertVerifiableEntityProperties(entity, signer);
		Assert.assertThat(entity.getVerifiableField1(), IsEqual.equalTo(7));
		Assert.assertThat(entity.getVerifiableField2(), IsEqual.equalTo(9));
		Assert.assertThat(entity.getNonVerifiableField1(), IsEqual.equalTo(-1));
		Assert.assertThat(entity.getSignature(), IsNull.nullValue());
	}

	@Test
	public void verifiableEntityWithNonVerifiableHierarchyCanBeRoundTripped() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockDerivedVerifiableEntityWithNonVerifiableData originalEntity = create(signer, 7, 9, 4, 5, 8, 2);
		final MockDerivedVerifiableEntityWithNonVerifiableData entity = new MockDerivedVerifiableEntityWithNonVerifiableData(
				VerifiableEntity.DeserializationOptions.VERIFIABLE,
				Utils.roundtripVerifiableEntity(originalEntity, new MockAccountLookup()));

		// Assert:
		assertVerifiableEntityProperties(entity, signer);
		Assert.assertThat(entity.getVerifiableField1(), IsEqual.equalTo(7));
		Assert.assertThat(entity.getVerifiableField2(), IsEqual.equalTo(9));
		Assert.assertThat(entity.getNonVerifiableField1(), IsEqual.equalTo(4));
		Assert.assertThat(entity.getVerifiableField3(), IsEqual.equalTo(5));
		Assert.assertThat(entity.getVerifiableField4(), IsEqual.equalTo(8));
		Assert.assertThat(entity.getNonVerifiableField2(), IsEqual.equalTo(2));
		Assert.assertThat(entity.getSignature(), IsNull.notNullValue());
	}

	@Test
	public void nonVerifiableEntityWithNonVerifiableHierarchyCanBeRoundTrippedWithoutNonVerifiableData() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockDerivedVerifiableEntityWithNonVerifiableData originalEntity = create(signer, 7, 9, 4, 5, 8, 2);
		final MockDerivedVerifiableEntityWithNonVerifiableData entity = new MockDerivedVerifiableEntityWithNonVerifiableData(
				VerifiableEntity.DeserializationOptions.NON_VERIFIABLE,
				Utils.roundtripSerializableEntity(originalEntity.asNonVerifiable(), new MockAccountLookup()));

		// Assert:
		assertVerifiableEntityProperties(entity, signer);
		Assert.assertThat(entity.getVerifiableField1(), IsEqual.equalTo(7));
		Assert.assertThat(entity.getVerifiableField2(), IsEqual.equalTo(9));
		Assert.assertThat(entity.getNonVerifiableField1(), IsEqual.equalTo(-1));
		Assert.assertThat(entity.getVerifiableField3(), IsEqual.equalTo(5));
		Assert.assertThat(entity.getVerifiableField4(), IsEqual.equalTo(8));
		Assert.assertThat(entity.getNonVerifiableField2(), IsEqual.equalTo(-1));
		Assert.assertThat(entity.getSignature(), IsNull.nullValue());
	}

	@Test
	public void nonVerifiableSerializationWithNonVerifiableDataExcludesNonVerifiableData() {
		// Arrange:
		final VerifiableEntity entity = create(Utils.generateRandomAccount(), 7, 9, 4);
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		entity.asNonVerifiable().serialize(serializer);
		final JSONObject object = serializer.getObject();

		// Assert:
		Assert.assertThat(object.containsKey("verifiableField1"), IsEqual.equalTo(true));
		Assert.assertThat(object.containsKey("verifiableField2"), IsEqual.equalTo(true));
		Assert.assertThat(object.containsKey("nonVerifiableField1"), IsEqual.equalTo(false));
	}

	@Test
	public void nonVerifiableSerializationWithNonVerifiableHierarchyExcludesNonVerifiableData() {
		// Arrange:
		final VerifiableEntity entity = create(Utils.generateRandomAccount(), 7, 9, 4, 5, 8, 2);
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		entity.asNonVerifiable().serialize(serializer);
		final JSONObject object = serializer.getObject();

		// Assert:
		Assert.assertThat(object.containsKey("verifiableField1"), IsEqual.equalTo(true));
		Assert.assertThat(object.containsKey("verifiableField2"), IsEqual.equalTo(true));
		Assert.assertThat(object.containsKey("verifiableField3"), IsEqual.equalTo(true));
		Assert.assertThat(object.containsKey("verifiableField4"), IsEqual.equalTo(true));
		Assert.assertThat(object.containsKey("nonVerifiableField1"), IsEqual.equalTo(false));
		Assert.assertThat(object.containsKey("nonVerifiableField2"), IsEqual.equalTo(false));
	}

	private static MockVerifiableEntityWithNonVerifiableData create(
			final Account signer,
			final int verifiableField1,
			final int verifiableField2,
			final int nonVerifiableField1) {
		return new MockVerifiableEntityWithNonVerifiableData(signer, verifiableField1, verifiableField2, nonVerifiableField1);
	}

	private static MockDerivedVerifiableEntityWithNonVerifiableData create(
			final Account signer,
			final int verifiableField1,
			final int verifiableField2,
			final int nonVerifiableField1,
			final int verifiableField3,
			final int verifiableField4,
			final int nonVerifiableField2) {
		return new MockDerivedVerifiableEntityWithNonVerifiableData(
				signer,
				verifiableField1,
				verifiableField2,
				nonVerifiableField1,
				verifiableField3,
				verifiableField4,
				nonVerifiableField2);
	}

	private static void assertVerifiableEntityProperties(
			final MockVerifiableEntityWithNonVerifiableData entity,
			final Account signer) {
		// Assert:
		Assert.assertThat(entity.getType(), IsEqual.equalTo(MockVerifiableEntityWithNonVerifiableData.TYPE));
		Assert.assertThat(entity.getVersion(), IsEqual.equalTo(0x98000000 | MockVerifiableEntityWithNonVerifiableData.VERSION));
		Assert.assertThat(entity.getTimeStamp(), IsEqual.equalTo(MockVerifiableEntityWithNonVerifiableData.TIMESTAMP));
		Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signer));
	}

	//endregion

	//region factory functions

	private static MockVerifiableEntity createRoundTrippedEntity(
			final Account originalSigner,
			final int customField,
			final Account deserializedSigner) {
		// Act:
		final MockVerifiableEntity originalEntity = new MockVerifiableEntity(originalSigner, customField);
		return createRoundTrippedEntity(originalEntity, deserializedSigner);
	}

	private static MockVerifiableEntity createRoundTrippedEntity(
			final MockVerifiableEntity originalEntity,
			final Account deserializedSigner) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalEntity, deserializedSigner);
		return new MockVerifiableEntity(deserializer);
	}

	private static MockVerifiableEntity createRoundTrippedEntity(
			final Account originalSigner,
			final int customField,
			final AccountLookup accountLookup) {
		// Arrange:
		final MockVerifiableEntity originalEntity = new MockVerifiableEntity(originalSigner, customField);
		originalEntity.sign();

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalEntity, accountLookup);
		return new MockVerifiableEntity(deserializer);
	}

	private static MockVerifiableEntity createNonVerifiableRoundTrippedEntity(
			final MockVerifiableEntity originalEntity,
			final Account deserializedSigner) {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(deserializedSigner);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalEntity.asNonVerifiable(), accountLookup);
		return new MockVerifiableEntity(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
	}

	//endregion

	//region mock classes

	private static class MockVerifiableEntityWithCustomVersion extends VerifiableEntity {

		public MockVerifiableEntityWithCustomVersion(final int version) {
			super(111, version, TimeInstant.ZERO, Utils.generateRandomAccount());
		}

		@Override
		protected void serializeImpl(final Serializer serializer) {
		}
	}

	private static class MockVerifiableEntityWithNonVerifiableData extends VerifiableEntity {
		public static final int TYPE = 12;
		public static final int VERSION = 24;
		public static final TimeInstant TIMESTAMP = new TimeInstant(127435);

		private int verifiableField1;
		private final int verifiableField2;
		private int nonVerifiableField1;

		public MockVerifiableEntityWithNonVerifiableData(
				final Account signer,
				final int verifiableField1,
				final int verifiableField2,
				final int nonVerifiableField1) {
			super(TYPE, VERSION, TIMESTAMP, signer);
			this.verifiableField1 = verifiableField1;
			this.verifiableField2 = verifiableField2;
			this.nonVerifiableField1 = nonVerifiableField1;
		}

		public MockVerifiableEntityWithNonVerifiableData(final DeserializationOptions options, final Deserializer deserializer) {
			super(deserializer.readInt("type"), options, deserializer);
			this.verifiableField1 = deserializer.readInt("verifiableField1");
			this.verifiableField2 = deserializer.readInt("verifiableField2");

			this.nonVerifiableField1 = DeserializationOptions.VERIFIABLE == options
					? deserializer.readInt("nonVerifiableField1")
					: -1;
		}

		public int getVerifiableField1() {
			return this.verifiableField1;
		}

		public int getVerifiableField2() {
			return this.verifiableField2;
		}

		public int getNonVerifiableField1() {
			return this.nonVerifiableField1;
		}

		@Override
		protected void serializeImpl(final Serializer serializer) {
			// this shouldn't be called since the other overload is implemented
		}

		@Override
		protected void serializeImpl(final Serializer serializer, final boolean includeNonVerifiableData) {
			serializer.writeInt("verifiableField1", this.verifiableField1);
			serializer.writeInt("verifiableField2", this.verifiableField2);

			if (includeNonVerifiableData) {
				serializer.writeInt("nonVerifiableField1", this.nonVerifiableField1);
			}
		}

		public void setVerifiableField1(final int value) {
			this.verifiableField1 = value;
		}

		public void setNonVerifiableField1(final int value) {
			this.nonVerifiableField1 = value;
		}
	}

	private static class MockDerivedVerifiableEntityWithNonVerifiableData extends MockVerifiableEntityWithNonVerifiableData {
		private final int verifiableField3;
		private final int verifiableField4;
		private final int nonVerifiableField2;

		public MockDerivedVerifiableEntityWithNonVerifiableData(
				final Account signer,
				final int verifiableField1,
				final int verifiableField2,
				final int nonVerifiableField1,
				final int verifiableField3,
				final int verifiableField4,
				final int nonVerifiableField2) {
			super(signer, verifiableField1, verifiableField2, nonVerifiableField1);
			this.verifiableField3 = verifiableField3;
			this.verifiableField4 = verifiableField4;
			this.nonVerifiableField2 = nonVerifiableField2;
		}

		public MockDerivedVerifiableEntityWithNonVerifiableData(final DeserializationOptions options, final Deserializer deserializer) {
			super(options, deserializer);
			this.verifiableField3 = deserializer.readInt("verifiableField3");
			this.verifiableField4 = deserializer.readInt("verifiableField4");

			this.nonVerifiableField2 = DeserializationOptions.VERIFIABLE == options
					? deserializer.readInt("nonVerifiableField2")
					: -1;
		}

		public int getVerifiableField3() {
			return this.verifiableField3;
		}

		public int getVerifiableField4() {
			return this.verifiableField4;
		}

		public int getNonVerifiableField2() {
			return this.nonVerifiableField2;
		}

		@Override
		protected void serializeImpl(final Serializer serializer, final boolean includeNonVerifiableData) {
			super.serializeImpl(serializer, includeNonVerifiableData);
			serializer.writeInt("verifiableField3", this.verifiableField3);
			serializer.writeInt("verifiableField4", this.verifiableField4);

			if (includeNonVerifiableData) {
				serializer.writeInt("nonVerifiableField2", this.nonVerifiableField2);
			}
		}
	}

	//endregion
}