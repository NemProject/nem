package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.security.InvalidParameterException;

public class VerifiableEntityTest {

    //region New

    @Test
    public void ctorCanCreateEntityForAccountWithSignerPrivateKey() {
        // Arrange:
        final KeyPair publicPrivateKeyPair = new KeyPair();
        final Account signer = new Account(publicPrivateKeyPair);

		// Act:
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 6);

        // Assert:
        Assert.assertThat(entity.getType(), IsEqual.equalTo(MockVerifiableEntity.TYPE));
        Assert.assertThat(entity.getVersion(), IsEqual.equalTo(MockVerifiableEntity.VERSION));
        Assert.assertThat(entity.getCustomField(), IsEqual.equalTo(6));
        Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(entity.getSignature(), IsEqual.equalTo(null));
    }

    @Test
    public void ctorCanCreateEntityForAccountWithoutSignerKey() {
        // Arrange:
		final Address address = Address.fromEncoded("Alpha");

        // Act:
        new MockVerifiableEntity(new Account(address));
    }

    //endregion

    //region Serialization

    @Test
    public void entityCanBeRoundTripped() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
        final MockVerifiableEntity originalEntity = new MockVerifiableEntity(signer, 7);
        final MockVerifiableEntity entity = createRoundTrippedEntity(originalEntity, signerPublicKeyOnly);

        // Assert:
        Assert.assertThat(entity.getType(), IsEqual.equalTo(MockVerifiableEntity.TYPE));
        Assert.assertThat(entity.getVersion(), IsEqual.equalTo(MockVerifiableEntity.VERSION));
        Assert.assertThat(entity.getCustomField(), IsEqual.equalTo(7));
        Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signerPublicKeyOnly));
		Assert.assertThat(entity.getSignature(), IsNot.not(IsEqual.equalTo(null)));
    }

    @Test
    public void roundTrippedEntityCanBeVerified() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
        final MockVerifiableEntity entity = createRoundTrippedEntity(signer, 7, signerPublicKeyOnly);

        // Assert:
        Assert.assertThat(entity.verify(), IsEqual.equalTo(true));
    }

    @Test(expected = SerializationException.class)
    public void serializeFailsIfSignatureIsNotPresent() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer);

        // Act:
        entity.serialize(new JsonSerializer());
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
        Assert.assertThat(entity.getSignature(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(entity.verify(), IsEqual.equalTo(true));
    }

    @Test
    public void changingFieldInvalidatesSignature() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);

        // Act:
        entity.sign();
        entity.setCustomField(12);

        // Assert:
        Assert.assertThat(entity.getSignature(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(entity.verify(), IsEqual.equalTo(false));
    }

    @Test(expected = InvalidParameterException.class)
    public void cannotSignWithoutPrivateKey() {
        // Arrange:
        final Address address = Address.fromEncoded("Gamma");
        final Account signer = new MockAccount(address);
        final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
        final MockVerifiableEntity entity = createRoundTrippedEntity(signer, 7, signerPublicKeyOnly);

        // Assert:
        entity.sign();
    }

	@Test(expected = InvalidParameterException.class)
	public void cannotSignWithoutKey() {
		// Arrange:
		final Address address = Address.fromEncoded("Beta");
		final MockVerifiableEntity entity = new MockVerifiableEntity(new Account(address));

		// Act:
		entity.sign();
	}

	@Test(expected = InvalidParameterException.class)
	public void cannotVerifyWithoutKey() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account fakeSigner = new Account(Address.fromEncoded("Alpha"));
		final MockVerifiableEntity entity = new MockVerifiableEntity(signer);
		final MockVerifiableEntity fakeEntity = new MockVerifiableEntity(fakeSigner);

		// Act:
		entity.sign();
		fakeEntity.setSignature(entity.getSignature());

		fakeEntity.verify();
	}


	@Test(expected = CryptoException.class)
    public void cannotVerifyWithoutSignature() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);

        // Act:
        entity.verify();
    }

    //endregion

    private MockVerifiableEntity createRoundTrippedEntity(
        final Account originalSigner,
        final int customField,
        final Account deserializedSigner) {
        // Act:
        final MockVerifiableEntity originalEntity = new MockVerifiableEntity(originalSigner, customField);
        return createRoundTrippedEntity(originalEntity, deserializedSigner);
    }

    private MockVerifiableEntity createRoundTrippedEntity(
        MockVerifiableEntity originalEntity,
        final Account deserializedSigner) {
        // Act:
        Deserializer deserializer = Utils.roundtripVerifiableEntity(originalEntity, deserializedSigner);
        return new MockVerifiableEntity(deserializer);
    }
}
