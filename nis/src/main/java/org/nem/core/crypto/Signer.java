package org.nem.core.crypto;

import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

public class Signer {

    private final KeyPair keyPair;

    public Signer(final KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public Signature sign(byte[] input) {
        ECDSASigner signer = new ECDSASigner();
        ECPrivateKeyParameters privateKeyParams = getPrivateKeyParameters();
        signer.init(true, privateKeyParams);
        BigInteger[] components = signer.generateSignature(input);
        final Signature signature = new Signature(components[0], components[1]);
        signature.makeCanonical();
        return signature;
    }

    private ECPrivateKeyParameters getPrivateKeyParameters() {
        return new ECPrivateKeyParameters(keyPair.getPrivateKey(), Curves.secp256k1().getParams());
    }

    public boolean verify(byte[] data, Signature signature) {
        if (!signature.isCanonical())
            return false;

        ECDSASigner signer = new ECDSASigner();
        ECPublicKeyParameters params = getPublicKeyParameters();
        signer.init(false, params);
        return signer.verifySignature(data, signature.getR(), signature.getS());
    }

    private ECPublicKeyParameters getPublicKeyParameters() {
        ECPoint point = Curves.secp256k1().getParams().getCurve().decodePoint(keyPair.getPublicKey());
        return new ECPublicKeyParameters(point, Curves.secp256k1().getParams());
    }
}
