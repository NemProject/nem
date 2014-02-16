package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;

public class KeyPairTest {

    @Test
    public void ctorCanCreateNewKeyPair() throws Exception {
        // Act:
        KeyPair kp = new KeyPair();

        // Assert:
        Assert.assertThat(kp.hasPrivateKey(), IsEqual.equalTo(true));
        Assert.assertThat(kp.getPrivateKey(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(kp.hasPublicKey(), IsEqual.equalTo(true));
        Assert.assertThat(kp.getPublicKey(), IsNot.not(IsEqual.equalTo(null)));
    }

    @Test
    public void ctorCanCreateNewKeyPairWithCompressedPublicKey() throws Exception {
        // Act:
        KeyPair kp = new KeyPair();

        // Assert:
        Assert.assertThat(kp.getPublicKey().length, IsEqual.equalTo(33));
    }

    @Test
    public void ctorCreatesDifferentInstancesWithDifferentKeys() throws Exception {
        // Act:
        KeyPair kp1 = new KeyPair();
        KeyPair kp2 = new KeyPair();

        // Assert:
        Assert.assertThat(kp2.getPrivateKey(), IsNot.not(IsEqual.equalTo(kp1.getPrivateKey())));
        Assert.assertThat(kp2.getPublicKey(), IsNot.not(IsEqual.equalTo(kp1.getPublicKey())));
    }

    @Test
    public void ctorCanCreateKeyPairAroundPrivateKey() throws Exception {
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
    public void ctorCanCreateKeyPairAroundPublicKey() throws Exception {
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
}
