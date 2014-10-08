package org.nem.core.crypto.ed25519;

import org.apache.commons.math3.FieldElement;
import org.nem.core.crypto.ed25519.arithmetic.Ed25519GroupElement;

import java.io.Serializable;

/**
 * A twisted Edwards curve.
 * Points on the curve satisfy -x^2 + y^2 = 1 + d x^2y^2
 * @author str4d
 *
 */
public class Curve implements Serializable {
    private static final long serialVersionUID = 4578920872509827L;
    private final Field f;
    private final FieldElement d;
    private final FieldElement d2;
    private final FieldElement I;

    private final Ed25519GroupElement zeroP2;
    private final Ed25519GroupElement zeroP3;
    private final Ed25519GroupElement zeroPrecomp;

    public Curve(Field f, byte[] d, FieldElement I) {
        this.f = f;
        this.d = f.fromByteArray(d);
        this.d2 = this.d.add(this.d);
        this.I = I;

        FieldElement zero = f.ZERO;
        FieldElement one = f.ONE;
        zeroP2 = Ed25519GroupElement.p2(this, zero, one, one);
        zeroP3 = Ed25519GroupElement.p3(this, zero, one, one, zero);
        zeroPrecomp = Ed25519GroupElement.precomp(this, one, one, zero);
    }

    public Field getField() {
        return f;
    }

    public FieldElement getD() {
        return d;
    }

    public FieldElement get2D() {
        return d2;
    }

    public FieldElement getI() {
        return I;
    }

    public Ed25519GroupElement getZero(Ed25519GroupElement.Representation repr) {
        switch (repr) {
        case P2:
            return zeroP2;
        case P3:
            return zeroP3;
        case PRECOMP:
            return zeroPrecomp;
        default:
            return null;
        }
    }

    public Ed25519GroupElement createPoint(byte[] P, boolean precompute) {
        Ed25519GroupElement ge = new Ed25519GroupElement(this, P);
        if (precompute)
            ge.precompute(true);
        return ge;
    }
}
