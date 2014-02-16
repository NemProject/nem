package org.nem.core.crypto;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.SecureRandom;

public class KeyPair {
    private final static SecureRandom RANDOM;

    static {
        RANDOM = new SecureRandom();
    }

    private final BigInteger privateKey;
    private final byte[] publicKey;

    public KeyPair() {
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        ECKeyGenerationParameters keyGenParams = new ECKeyGenerationParameters(Curves.secp256k1().getParams(), RANDOM);
        generator.init(keyGenParams);

        AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
        ECPrivateKeyParameters privateKeyParams = (ECPrivateKeyParameters)keyPair.getPrivate();
        ECPublicKeyParameters publicKeyParams = (ECPublicKeyParameters)keyPair.getPublic();
        this.privateKey = privateKeyParams.getD();

        ECPoint point = publicKeyParams.getQ();
        this.publicKey = point.getEncoded(true);
    }

    public KeyPair(final BigInteger privateKey) {
        this(privateKey, publicKeyFromPrivateKey(privateKey));
    }

    public KeyPair(final byte[] publicKey) {
        this(null, publicKey);
    }

    private KeyPair(final BigInteger privateKey, final byte[] publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    private static byte[] publicKeyFromPrivateKey(final BigInteger privateKey) {
        ECPoint point = Curves.secp256k1().getParams().getG().multiply(privateKey);
        return point.getEncoded(true);
    }

    public BigInteger getPrivateKey() {

        return this.privateKey;
    }

    public byte[] getPublicKey() {
        return this.publicKey;
    }

    public Boolean hasPrivateKey() { return null != this.privateKey; }

    public Boolean hasPublicKey() { return null != this.publicKey; }

    public ECPrivateKeyParameters getPrivateKeyParameters() {
        return new ECPrivateKeyParameters(this.getPrivateKey(), Curves.secp256k1().getParams());
    }

    public ECPublicKeyParameters getPublicKeyParameters() {
        ECPoint point = Curves.secp256k1().getParams().getCurve().decodePoint(this.getPublicKey());
        return new ECPublicKeyParameters(point, Curves.secp256k1().getParams());
    }
}