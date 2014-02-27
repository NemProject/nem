package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.test.Utils;

public class MessageTest {

    @Test
    public void unencryptedMessageCanBeCreated() {
        // Arrange:
        final byte[] input = Utils.generateRandomBytes();

        // Act:
        final Message message = new Message(null, null, input);

        // Assert:
        Assert.assertThat(message.getRawMessage(), IsEqual.equalTo(input));
    }

    @Test
    public void encryptedMessageCanBeCreated() {
        // Arrange:
        final KeyPair skp = new KeyPair();
        final KeyPair rkp = new KeyPair();
        final Cipher cipher = new Cipher(skp, rkp);
        final byte[] input = Utils.generateRandomBytes();
        final byte[] encryptedInput = cipher.encrypt(input);

        // Act:
        final Message message = new Message(rkp, skp.getPublicKey(), encryptedInput);

        // Assert:
        Assert.assertThat(message.getRawMessage(), IsEqual.equalTo(encryptedInput));
        Assert.assertThat(message.getDecryptedMessage(), IsEqual.equalTo(input));
    }
}
