package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

public class CipherTest {

    @Test
    public void encryptedDataCanBeDecrypted() throws Exception {
        // Arrange:
        KeyPair kp = new KeyPair();
        Cipher cipher = new Cipher(kp, kp);
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] encryptedBytes = cipher.encrypt(input);
        byte[] decryptedBytes = cipher.decrypt(encryptedBytes);

        // Assert:
        Assert.assertThat(encryptedBytes, IsNot.not(IsEqual.equalTo(decryptedBytes)));
        Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
    }

    @Test
    public void dataCanBeEncryptedWithSenderPrivateKeyAndRecipientPublicKey() throws Exception {
        // Arrange:
        KeyPair skp = new KeyPair();
        KeyPair rkp = new KeyPair();
        Cipher cipher = new Cipher(skp, new KeyPair(rkp.getPublicKey()));
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] encryptedBytes = cipher.encrypt(input);

        // Assert:
        Assert.assertThat(encryptedBytes, IsNot.not(IsEqual.equalTo(input)));
    }

    @Test
    public void dataCanBeDecryptedWithSenderPublicKeyAndRecipientPrivateKey() throws Exception {
        // Arrange:
        KeyPair skp = new KeyPair();
        KeyPair rkp = new KeyPair();
        Cipher cipher1 = new Cipher(skp, new KeyPair(rkp.getPublicKey()));
        Cipher cipher2 = new Cipher(new KeyPair(skp.getPublicKey()), rkp);
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] encryptedBytes = cipher1.encrypt(input);
        byte[] decryptedBytes = cipher2.decrypt(encryptedBytes);

        // Assert:
        Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
    }

    @Test
    public void dataEncryptedWithPrivateKeyCanOnlyBeDecryptedByMatchingPublicKey() throws Exception {
        // Arrange:
        Cipher cipher1 = new Cipher(new KeyPair(), new KeyPair());
        Cipher cipher2 = new Cipher(new KeyPair(), new KeyPair());
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] encryptedBytes1 = cipher1.encrypt(input);
        byte[] encryptedBytes2 = cipher2.encrypt(input);

        // Assert:
        Assert.assertThat(cipher1.decrypt(encryptedBytes1), IsEqual.equalTo(input));
        Assert.assertThat(cipher1.decrypt(encryptedBytes2), IsEqual.equalTo(null));
        Assert.assertThat(cipher2.decrypt(encryptedBytes1), IsEqual.equalTo(null));
        Assert.assertThat(cipher2.decrypt(encryptedBytes2), IsEqual.equalTo(input));
    }
}
