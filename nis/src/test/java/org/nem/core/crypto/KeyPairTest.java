package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;

import java.security.InvalidParameterException;

public class KeyPairTest {

    @Test
    public void ctorCanCreateNewKeyPair() {
        // Act:
        KeyPair kp = new KeyPair();

        // Assert:
        Assert.assertThat(kp.hasPrivateKey(), IsEqual.equalTo(true));
        Assert.assertThat(kp.getPrivateKey(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(kp.hasPublicKey(), IsEqual.equalTo(true));
        Assert.assertThat(kp.getPublicKey(), IsNot.not(IsEqual.equalTo(null)));
    }

    @Test
    public void ctorCanCreateNewKeyPairWithCompressedPublicKey() {
        // Act:
        KeyPair kp = new KeyPair();

        // Assert:
        Assert.assertThat(kp.getPublicKey().getRaw().length, IsEqual.equalTo(33));
    }

    @Test
    public void ctorCreatesDifferentInstancesWithDifferentKeys() {
        // Act:
        KeyPair kp1 = new KeyPair();
        KeyPair kp2 = new KeyPair();

        // Assert:
        Assert.assertThat(kp2.getPrivateKey(), IsNot.not(IsEqual.equalTo(kp1.getPrivateKey())));
        Assert.assertThat(kp2.getPublicKey(), IsNot.not(IsEqual.equalTo(kp1.getPublicKey())));
    }

    @Test
    public void ctorCanCreateKeyPairAroundPrivateKey() {
        // Arrange:
        KeyPair kp1 = new KeyPair();

        // Act:
        KeyPair kp2 = new KeyPair(kp1.getPrivateKey());

        // Assert:
        Assert.assertThat(kp2.hasPrivateKey(), IsEqual.equalTo(true));
        Assert.assertThat(kp2.getPrivateKey(), IsEqual.equalTo(kp1.getPrivateKey()));
        Assert.assertThat(kp2.hasPublicKey(), IsEqual.equalTo(true));
        Assert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
    }

    @Test
    public void ctorCanCreateKeyPairAroundPublicKey() {
        // Arrange:
        KeyPair kp1 = new KeyPair();

        // Act:
        KeyPair kp2 = new KeyPair(kp1.getPublicKey());

        // Assert:
        Assert.assertThat(kp2.hasPrivateKey(), IsEqual.equalTo(false));
        Assert.assertThat(kp2.getPrivateKey(), IsEqual.equalTo(null));
        Assert.assertThat(kp2.hasPublicKey(), IsEqual.equalTo(true));
        Assert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
    }

    @Test
    public void ctorFailsIfPublicKeyLengthIsWrong() {
        // Arrange:
        byte[] publicKey = (new KeyPair()).getPublicKey().getRaw();

        byte[] shortPublicKey = new byte[publicKey.length - 1];
        System.arraycopy(publicKey, 0, shortPublicKey, 0, shortPublicKey.length);

        byte[] longPublicKey = new byte[publicKey.length + 1];
        System.arraycopy(publicKey, 0, longPublicKey, 0, publicKey.length);

        // Assert:
        assertInvalidPublicKey(shortPublicKey);
        assertInvalidPublicKey(longPublicKey);
    }

    @Test
         public void ctorFailsIfPublicKeyFirstByteIsWrong() {
        // Arrange:
        byte[] publicKey = (new KeyPair()).getPublicKey().getRaw();

        byte[] smallBytePublicKey = new byte[publicKey.length];
        System.arraycopy(publicKey, 0, smallBytePublicKey, 0, publicKey.length);
        smallBytePublicKey[0] = 0x01;

        byte[] largeBytePublicKey = new byte[publicKey.length];
        System.arraycopy(publicKey, 0, largeBytePublicKey, 0, publicKey.length);
        largeBytePublicKey[0] = 0x04;

        // Assert:
        assertInvalidPublicKey(smallBytePublicKey);
        assertInvalidPublicKey(largeBytePublicKey);
    }

    private static void assertInvalidPublicKey(final byte[] publicKey) {
        try {
            // Act:
            new KeyPair(new PublicKey(publicKey));
            Assert.fail("No exception was thrown");
        } catch (InvalidParameterException ex) {
        }
    }
}
