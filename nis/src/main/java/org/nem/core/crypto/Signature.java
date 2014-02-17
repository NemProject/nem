package org.nem.core.crypto;

import java.math.BigInteger;

public class Signature {

    private BigInteger r;
    private BigInteger s;

    public Signature(final BigInteger r, final BigInteger s) {
        this.r = r;
        this.s = s;
    }

    public BigInteger getR() {
        return this.r;
    }

    public BigInteger getS() {
        return this.s;
    }

    public Boolean isCanonical() {
        return this.s.compareTo(Curves.secp256k1().getHalfCurveOrder()) <= 0;
    }

    public void makeCanonical() {
        if (!this.isCanonical())
            this.s = Curves.secp256k1().getParams().getN().subtract(this.s);
    }

    @Override
    public int hashCode() {
        return r.hashCode() ^ s.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Signature))
            return false;

        Signature rhs = (Signature)obj;
        return 0 == r.compareTo(rhs.r) && 0 == s.compareTo(rhs.s);
    }
}