package org.nem.core.crypto.ed25519.spec;

import org.nem.core.crypto.ed25519.Curve;
import org.nem.core.crypto.ed25519.arithmetic.*;

import java.io.Serializable;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Parameter specification for an EdDSA algorithm.
 * @author str4d
 *
 */
public class EdDSAParameterSpec implements AlgorithmParameterSpec, Serializable {
    private static final long serialVersionUID = 8274987108472012L;
    private final Curve curve;
    private final String hashAlgo;
    private final ScalarOps sc;
    private final GroupElement B;

    /**
     *  @throws IllegalArgumentException if hash algorithm is unsupported or length is wrong
     */
    public EdDSAParameterSpec(Curve curve, String hashAlgo,
            ScalarOps sc, GroupElement B) {
        try {
            MessageDigest hash = MessageDigest.getInstance(hashAlgo);
            // EdDSA hash function must produce 2b-bit output
            if (curve.getField().getb()/4 != hash.getDigestLength())
                throw new IllegalArgumentException("Hash output is not 2b-bit");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported hash algorithm");
        }

        this.curve = curve;
        this.hashAlgo = hashAlgo;
        this.sc = sc;
        this.B = B;
    }

    public Curve getCurve() {
        return curve;
    }

    public String getHashAlgorithm() {
        return hashAlgo;
    }

    public ScalarOps getScalarOps() {
        return sc;
    }

    public GroupElement getB() {
        return B;
    }
}
