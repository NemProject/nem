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
        Assert.assertThat(entity.getType(), IsEqual.equalTo(11));
        Assert.assertThat(entity.getVersion(), IsEqual.equalTo(23));
        Assert.assertThat(entity.getCustomField(), IsEqual.equalTo(6));
        Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(entity.getSignature(), IsEqual.equalTo(null));
    }

    @Test(expected = InvalidParameterException.class)
    public void ctorCannotCreateEntityForAccountWithoutSignerPrivateKey() {
        // Arrange:
        final KeyPair publicPrivateKeyPair = new KeyPair();
        final KeyPair publicOnlyKeyPair = new KeyPair(publicPrivateKeyPair.getPublicKey());

        // Act:
        new MockVerifiableEntity(new Account(publicOnlyKeyPair));
    }

    //endregion

    //region Serialization

    @Test
    public void entityCanBeRoundTripped() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = new Account(new KeyPair(signer.getPublicKey()));
        final MockVerifiableEntity originalEntity = new MockVerifiableEntity(signer, 7);
        final MockVerifiableEntity entity = createRoundTrippedEntity(originalEntity, signerPublicKeyOnly);

        // Assert:
        Assert.assertThat(entity.getType(), IsEqual.equalTo(11));
        Assert.assertThat(entity.getVersion(), IsEqual.equalTo(23));
        Assert.assertThat(entity.getCustomField(), IsEqual.equalTo(7));
        Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signerPublicKeyOnly));
		Assert.assertThat(entity.getSignature(), IsNot.not(IsEqual.equalTo(null)));
    }

    @Test
    public void roundTrippedEntityCanBeVerified() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = new Account(new KeyPair(signer.getPublicKey()));
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
        entity.serialize(new DelegatingObjectSerializer(new JsonSerializer()));
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
        final Account signerPublicKeyOnly = new Account(new KeyPair(signer.getPublicKey()));
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
        ObjectDeserializer deserializer = Utils.RoundtripVerifiableEntity(originalEntity, deserializedSigner);
        return new MockVerifiableEntity(deserializer);
    }
}
