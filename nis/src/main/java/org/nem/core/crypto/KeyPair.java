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
     * Creates a random key pair.
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
     * Creates a key pair around a private key.
     * The public key is calculated from the private key.
     *
     * @param privateKey The private key.
     */
    public KeyPair(final BigInteger privateKey) {
        this(privateKey, publicKeyFromPrivateKey(privateKey));
    }

    /**
     * Creates a key pair around a public key.
     * The private key is empty.
     *
     * @param publicKey The public key.
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

    /**
     * Gets the private key.
     *
     * @return The private key.
     */
    public BigInteger getPrivateKey() {

        return this.privateKey;
    }

    /**
     * Gets the public key.
     *
     * @return the public key.
     */
    public byte[] getPublicKey() {
        return this.publicKey;
    }

    /**
     * Determines if the current key pair has a private key.
     *
     * @return true if the current key pair has a private key.
     */
    public boolean hasPrivateKey() { return null != this.privateKey; }

    /**
     * Determines if the current key pair has a public key.
     *
     * @return true if the current key pair has a public key.
     */
    public boolean hasPublicKey() { return null != this.publicKey; }

    /**
     * Gets the EC private key parameters.
     *
     * @return The EC private key parameters.
     */
    public ECPrivateKeyParameters getPrivateKeyParameters() {
        return new ECPrivateKeyParameters(this.getPrivateKey(), Curves.secp256k1().getParams());
    }

    /**
     * Gets the EC public key parameters.
     *
     * @return The EC public key parameters.
     */
    public ECPublicKeyParameters getPublicKeyParameters() {
        ECPoint point = Curves.secp256k1().getParams().getCurve().decodePoint(this.getPublicKey());
        return new ECPublicKeyParameters(point, Curves.secp256k1().getParams());
    }

    private static boolean isPublicKeyCompressed(byte[] publicKey) {
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