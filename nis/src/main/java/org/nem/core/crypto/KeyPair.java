package org.nem.core.crypto;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.SecureRandom;

public class KeyPair {
    final static SecureRandom RANDOM;

    static {
        RANDOM = new SecureRandom();
    }

    public BigInteger privateKey;
    public byte[] publicKey;

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

    public KeyPair(BigInteger privateKey) {
        this(privateKey, publicKeyFromPrivateKey(privateKey));
    }

    public KeyPair(byte[] publicKey) {
        this(null, publicKey);
    }

    private KeyPair(BigInteger privateKey, byte[] publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public static byte[] publicKeyFromPrivateKey(BigInteger privateKey) {
        ECPoint point = Curves.secp256k1().getParams().getG().multiply(privateKey);
        return point.getEncoded(true);
    }

    public BigInteger getPrivateKey() {
        return this.privateKey;
    }

    public byte[] getPublicKey() {
        return this.publicKey;
    }

    public Boolean hasPrivateKey() {
        return null != this.privateKey;
    }

    public Boolean hasPublicKey() {
        return null != this.publicKey;
    }
}