package org.nem.core.crypto;

import org.nem.core.serialization.*;
import org.nem.core.utils.HexEncoder;

import java.math.BigInteger;

/**
 * Represents a private key.
 */
public class PrivateKey implements SerializableEntity {

    private final BigInteger value;

    /**
     * Creates a new private key.
     *
     * @param value The raw private key value.
     */
    public PrivateKey(final BigInteger value) {
        this.value = value;
    }

    /**
     * Deserializes a private key.
     *
     * @param deserializer The deserializer.
     */
    public PrivateKey(final Deserializer deserializer) {
        this.value = deserializer.readBigInteger("value");
    }

    /**
     * Gets the raw private key value.
     *
     * @return The raw private key value.
     */
    public BigInteger getRaw() { return this.value; }

    @Override
    public void serialize(final Serializer serializer) {
        serializer.writeBigInteger("value", this.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PrivateKey))
            return false;

        final PrivateKey rhs = (PrivateKey)obj;
        return this.value.equals(rhs.value);
    }

    @Override
    public String toString() {
        return HexEncoder.getString(this.value.toByteArray());
    }

    /**
     * Creates a private key from a hex string.
     *
     * @param hex The hex string.
     * @return The new private key.
     */
    public static PrivateKey fromHexString(final String hex) {
        try {
            // HexEncoder requires an even-length string
            final String paddedHex = 0 == hex.length() % 2 ? hex : "0" + hex;
            return new PrivateKey(new BigInteger(HexEncoder.getBytes(paddedHex)));
        } catch (SerializationException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Creates a private key from a decimal string.
     *
     * @param decimal The decimal string.
     * @return The new private key.
     */
    public static PrivateKey fromDecimalString(final String decimal) {
        try {
            return new PrivateKey(new BigInteger(decimal, 10));
        } catch (NumberFormatException e) {
            throw new CryptoException(e);
        }
    }

    private static PrivateKey fromString(final String s, final int base) {
        try {
            return new PrivateKey(new BigInteger(s, base));
        } catch (NumberFormatException e) {
            throw new CryptoException(e);
        }
    }
}
