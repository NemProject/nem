package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import java.math.BigInteger;

public class SignatureTest {

    @Test
    public void constructorInitializesFields() {
        // Arrange:
        BigInteger r = new BigInteger("99512345");
        BigInteger s = new BigInteger("12351234");

        // Act:
        Signature signature = new Signature(r, s);

        // Assert:
        Assert.assertThat(signature.getR(), IsEqual.equalTo(r));
        Assert.assertThat(signature.getS(), IsEqual.equalTo(s));
    }

    @Test
    public void isCanonicalReturnsTrueForCanonicalSignature() {
        // Arrange:
        Signature signature = createCanonicalSignature();

        // Assert:
        Assert.assertThat(signature.isCanonical(), IsEqual.equalTo(true));
    }

    @Test
    public void isCanonicalReturnsFalseForNonCanonicalSignature() {
        // Arrange:
        Signature signature = makeNonCanonical(createCanonicalSignature());

        // Assert:
        Assert.assertThat(signature.isCanonical(), IsEqual.equalTo(false));
    }

    @Test
    public void makeCanonicalMakesNonCanonicalSignatureCanonical() {
        // Arrange:
        Signature signature = createCanonicalSignature();
        Signature nonCanonicalSignature = makeNonCanonical(signature);

        Assert.assertThat(nonCanonicalSignature.isCanonical(), IsEqual.equalTo(false));

        // Act:
        nonCanonicalSignature.makeCanonical();

        // Assert:
        Assert.assertThat(nonCanonicalSignature.isCanonical(), IsEqual.equalTo(true));
        Assert.assertThat(nonCanonicalSignature.getR(), IsEqual.equalTo(signature.getR()));
        Assert.assertThat(nonCanonicalSignature.getS(), IsEqual.equalTo(signature.getS()));
    }

    private static Signature createCanonicalSignature() {
        // Arrange:
        KeyPair kp = new KeyPair();
        Signer signer = new Signer(kp);
        byte[] input = Utils.generateRandomBytes();

        // Act:
        return signer.sign(input);
    }

    private static Signature makeNonCanonical(Signature signature) {
        // Act:
        BigInteger nonCanonicalS = Curves.secp256k1().getParams().getN().subtract(signature.getS());
        return new Signature(signature.getR(), nonCanonicalS);
    }
}
