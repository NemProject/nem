package org.nem.core.crypto;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;

import java.math.BigInteger;

public class Signer {

    private final KeyPair keyPair;

    public Signer(final KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public Signature sign(final byte[] data) throws Exception {
        ECDSASigner signer = createECDSASigner();
        signer.init(true, this.keyPair.getPrivateKeyParameters());
        final byte[] hash = Hashes.sha3(data);
        BigInteger[] components = signer.generateSignature(hash);
        final Signature signature = new Signature(components[0], components[1]);
        signature.makeCanonical();
        return signature;
    }

    public boolean verify(final byte[] data, final Signature signature) throws Exception {
        if (!signature.isCanonical())
            return false;

        ECDSASigner signer = createECDSASigner();
        signer.init(false, this.keyPair.getPublicKeyParameters());
        final byte[] hash = Hashes.sha3(data);
        return signer.verifySignature(hash, signature.getR(), signature.getS());
    }

    private ECDSASigner createECDSASigner() {
        return new ECDSASigner(new HMacDSAKCalculator(new SHA3Digest()));
    }
}
