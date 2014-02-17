package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;

public class SignerTest {

    @Test
    public void signedDataCanBeVerified() throws Exception {
        // Arrange:
        KeyPair kp = new KeyPair();
        Signer signer = new Signer(kp);
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Signature signature = signer.sign(input);

        // Assert:
        Assert.assertThat(signer.verify(input, signature), IsEqual.equalTo(true));
    }

    @Test
    public void dataSignedWithKeyPairCannotBeVerifiedWithDifferentKeyPair() throws Exception {
        // Arrange:
        KeyPair kp1 = new KeyPair();
        KeyPair kp2 = new KeyPair();
        Signer signer1 = new Signer(kp1);
        Signer signer2 = new Signer(kp2);
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Signature signature1 = signer1.sign(input);
        Signature signature2 = signer2.sign(input);

        // Assert:
        Assert.assertThat(signer1.verify(input, signature1), IsEqual.equalTo(true));
        Assert.assertThat(signer1.verify(input, signature2), IsEqual.equalTo(false));
        Assert.assertThat(signer2.verify(input, signature1), IsEqual.equalTo(false));
        Assert.assertThat(signer2.verify(input, signature2), IsEqual.equalTo(true));
    }

    @Test
    public void verifyReturnsFalseForNonCanonicalSignature() throws Exception {
        // Arrange:
        KeyPair kp = new KeyPair();
        Signer signer = new Signer(kp);
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Signature signature = signer.sign(input);
        BigInteger nonCanonicalS = Curves.secp256k1().getParams().getN().subtract(signature.getS());
        Signature nonCanonicalSignature = new Signature(signature.getR(), nonCanonicalS);

        // Assert:
        Assert.assertThat(signer.verify(input, nonCanonicalSignature), IsEqual.equalTo(false));
    }
}
