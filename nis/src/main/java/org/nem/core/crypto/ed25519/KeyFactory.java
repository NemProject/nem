package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.ed25519.spec.*;

import java.security.*;
import java.security.spec.*;

/**
 * @author str4d
 *
 */
public class KeyFactory extends KeyFactorySpi {

    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
            throws InvalidKeySpecException {
        if (keySpec instanceof EdDSAPrivateKeySpec) {
            return new EdDSAPrivateKey((EdDSAPrivateKeySpec) keySpec);
        }
        throw new InvalidKeySpecException("key spec not recognised");
    }

    protected PublicKey engineGeneratePublic(KeySpec keySpec)
            throws InvalidKeySpecException {
        if (keySpec instanceof EdDSAPublicKeySpec) {
            return new EdDSAPublicKey((EdDSAPublicKeySpec) keySpec);
        }
        throw new InvalidKeySpecException("key spec not recognised");
    }

    @SuppressWarnings("unchecked")
    protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec)
            throws InvalidKeySpecException {
        if (keySpec.isAssignableFrom(EdDSAPublicKeySpec.class) && key instanceof EdDSAPublicKey) {
            EdDSAPublicKey k = (EdDSAPublicKey) key;
            if (k.getParams() != null) {
                return (T) new EdDSAPublicKeySpec(k.getA(), k.getParams());
            }
        } else if (keySpec.isAssignableFrom(EdDSAPrivateKeySpec.class) && key instanceof EdDSAPrivateKey) {
            EdDSAPrivateKey k = (EdDSAPrivateKey) key;
            if (k.getParams() != null) {
                return (T) new EdDSAPrivateKeySpec(k.getSeed(), k.getH(), k.geta(), k.getA(), k.getParams());
            }
        }
        throw new InvalidKeySpecException("not implemented yet " + key + " " + keySpec);
    }

    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        throw new InvalidKeyException("No other EdDSA key providers known");
    }
}
