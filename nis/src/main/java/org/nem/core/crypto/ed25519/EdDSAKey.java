package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.ed25519.spec.EdDSAParameterSpec;

/**
 * Common interface for all EdDSA keys.
 * @author str4d
 *
 */
public interface EdDSAKey {
    /**
     * return a parameter specification representing the EdDSA domain
     * parameters for the key.
     */
    public EdDSAParameterSpec getParams();
}
