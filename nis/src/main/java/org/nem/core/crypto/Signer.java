package org.nem.core.crypto;

import org.bouncycastle.crypto.signers.ECDSASigner;
import java.math.BigInteger;

public class Signer {

    private final KeyPair keyPair;

    public Signer(final KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public Signature sign(final byte[] input) {
        ECDSASigner signer = new ECDSASigner();
        signer.init(true, this.keyPair.getPrivateKeyParameters());
        BigInteger[] components = signer.generateSignature(input);
        final Signature signature = new Signature(components[0], components[1]);
        signature.makeCanonical();
        return signature;
    }

    public boolean verify(final byte[] data, final Signature signature) {
        if (!signature.isCanonical())
            return false;

        ECDSASigner signer = new ECDSASigner();
        signer.init(false, this.keyPair.getPublicKeyParameters());
        return signer.verifySignature(data, signature.getR(), signature.getS());
    }
}
