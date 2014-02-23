package org.nem.core.crypto;

import org.nem.core.utils.ArrayUtils;

import java.math.BigInteger;
import java.security.InvalidParameterException;

/**
 * A EC signature.
 */
public class Signature {

    private BigInteger r;
    private BigInteger s;

    /**
     * Creates a new signature.
     *
     * @param r The r-part of the signature.
     * @param s The s-part of the signature.
     */
    public Signature(final BigInteger r, final BigInteger s) {
        this.r = r;
        this.s = s;
    }

    /**
     * Creates a new signature.
     *
     * @param bytes The binary representation of the signature.
     */
    public Signature(final byte[] bytes) {
        if (64 != bytes.length)
            throw new InvalidParameterException("binary signature representation must be 64 bytes");

        byte[][] parts = ArrayUtils.split(bytes, 32);
        this.r = ArrayUtils.toBigInteger(parts[0]);
        this.s = ArrayUtils.toBigInteger(parts[1]);
    }

    /**
     * Gets the r-part of the signature.
     *
     * @return The r-part of the signature.
     */
    public BigInteger getR() {
        return this.r;
    }

    /**
     * Gets the s-part of the signature.
     *
     * @return The s-part of the signature.
     */
    public BigInteger getS() {
        return this.s;
    }

    /**
     * Determines if this signature is canonical.
     *
     * @return true if this signature is canonical.
     */
    public boolean isCanonical() {
        return this.s.compareTo(Curves.secp256k1().getHalfCurveOrder()) <= 0;
    }

    /**
     * Makes this signature canonical.
     */
    public void makeCanonical() {
        if (!this.isCanonical())
            this.s = Curves.secp256k1().getParams().getN().subtract(this.s);
    }

    /**
     * Gets a little-endian 64-byte representation of the signature.
     *
     * @return a little-endian 64-byte representation of the signature
     */
    public byte[] getBytes() {
        final byte[] rBytes = ArrayUtils.toByteArray(this.r, 32);
        final byte[] sBytes = ArrayUtils.toByteArray(this.s, 32);
        return ArrayUtils.concat(rBytes, sBytes);
    }

    @Override
    public int hashCode() {
        return this.r.hashCode() ^ this.s.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Signature))
            return false;

        Signature rhs = (Signature)obj;
        return 0 == this.r.compareTo(rhs.r) && 0 == this.s.compareTo(rhs.s);
    }
}