package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.ed25519.arithmetic.GroupElement;
import org.nem.core.crypto.ed25519.spec.*;

import java.security.PublicKey;

/**
 * An EdDSA public key.
 * @author str4d
 *
 */
public class EdDSAPublicKey implements EdDSAKey, PublicKey {
    private static final long serialVersionUID = 9837459837498475L;
    private final GroupElement A;
    private final GroupElement Aneg;
    private final byte[] Abyte;
    private final EdDSAParameterSpec edDsaSpec;

    public EdDSAPublicKey(EdDSAPublicKeySpec spec) {
        this.A = spec.getA();
        this.Aneg = spec.getNegativeA();
        this.Abyte = this.A.toByteArray();
        this.edDsaSpec = spec.getParams();
    }

    public String getAlgorithm() {
        return "EdDSA";
    }

    public String getFormat() {
        return "X.509";
    }

    public byte[] getEncoded() {
        // TODO Auto-generated method stub
        return null;
    }

    public EdDSAParameterSpec getParams() {
        return edDsaSpec;
    }

    public GroupElement getA() {
        return A;
    }

    public GroupElement getNegativeA() {
        return Aneg;
    }

    public byte[] getAbyte() {
        return Abyte;
    }
}
