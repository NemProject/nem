package org.nem.core.crypto;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.SecureRandom;

public class KeyPair {

    private final static int COMPRESSED_KEY_SIZE = 33;
    private final static SecureRandom RANDOM = new SecureRandom();

    private final BigInteger privateKey;
    private final byte[] publicKey;

    /**
     * Creates random key pair
     */
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

    /**
     * Create key pair. Public key is calculated from provided private key
     *
     * @param privateKey
     */
    public KeyPair(final BigInteger privateKey) {
        this(privateKey, publicKeyFromPrivateKey(privateKey));
    }

    /**
     * Create dummy key pair, where private key is empty.
     *
     * @param publicKey
     */
    public KeyPair(final byte[] publicKey) {
        this(null, publicKey);
    }

    private KeyPair(final BigInteger privateKey, final byte[] publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;

        if (!isPublicKeyCompressed(publicKey))
            throw new InvalidParameterException("publicKey must be in compressed form");
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

    private static Boolean isPublicKeyCompressed(byte[] publicKey) {
        if (COMPRESSED_KEY_SIZE != publicKey.length)
            return false;

        switch (publicKey[0]) {
            case 0x02:
            case 0x03:
                return true;
        }

        return false;
    }
}