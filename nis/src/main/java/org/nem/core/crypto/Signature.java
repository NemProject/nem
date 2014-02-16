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
}