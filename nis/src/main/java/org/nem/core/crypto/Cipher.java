package org.nem.core.crypto;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.IESEngine;
import org.bouncycastle.crypto.generators.KDF2BytesGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.macs.HMac;

public class Cipher {

    private final static IESParameters IES_PARAMETERS;

    static {
        byte[] d = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        byte[] e = new byte[] { 8, 7, 6, 5, 4, 3, 2, 1 };
        IES_PARAMETERS = new IESParameters(d, e, 64);
    }

    private final KeyPair senderKeyPair;
    private final KeyPair recipientKeyPair;
    private final IESEngine iesEncryptEngine;
    private final IESEngine iesDecryptEngine;

    public Cipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
        this.senderKeyPair = senderKeyPair;
        this.recipientKeyPair = recipientKeyPair;

        if (this.senderKeyPair.hasPrivateKey()) {
            this.iesEncryptEngine = createIesEngine();
            this.iesEncryptEngine.init(
                true,
                this.senderKeyPair.getPrivateKeyParameters(),
                this.recipientKeyPair.getPublicKeyParameters(),
                IES_PARAMETERS);
        } else {
            this.iesEncryptEngine = null;
        }

        if (this.recipientKeyPair.hasPrivateKey()) {
            this.iesDecryptEngine = createIesEngine();
            this.iesDecryptEngine.init(
                false,
                this.recipientKeyPair.getPrivateKeyParameters(),
                this.senderKeyPair.getPublicKeyParameters(),
                IES_PARAMETERS);
        } else {
            this.iesDecryptEngine = null;
        }
    }

    public byte[] encrypt(final byte[] input) throws Exception {
        return this.iesEncryptEngine.processBlock(input, 0, input.length);
    }

    public byte[] decrypt(final byte[] input) {
        try {
            return this.iesDecryptEngine.processBlock(input, 0, input.length);
        } catch (InvalidCipherTextException e) {
            return null;
        }
    }

    private static IESEngine createIesEngine() {
        return new IESEngine(
            new ECDHBasicAgreement(),
            new KDF2BytesGenerator(new SHA1Digest()),
            new HMac(new SHA1Digest()));
    }
}