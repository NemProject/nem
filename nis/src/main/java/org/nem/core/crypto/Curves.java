package org.nem.core.crypto;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;

import java.math.BigInteger;

public class Curves {

    static final Curve SECP256K1;

    static {
        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        ECDomainParameters ecParams = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
        SECP256K1 = new Curve(ecParams, ecParams.getN().shiftRight(1));
    }

    public static class Curve {
        final ECDomainParameters params;
        final BigInteger halfCurveOrder;

        private Curve(ECDomainParameters params, BigInteger halfCurveOrder) {
            this.params = params;
            this.halfCurveOrder = halfCurveOrder;
        }

        public ECDomainParameters getParams() {
            return this.params;
        }

        public BigInteger getHalfCurveOrder() {
            return this.halfCurveOrder;
        }
    }

    public static Curve secp256k1() {
        return SECP256K1;
    }
}
