package org.nem.core.crypto.ed25519.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * Implementation of AlgorithmParameterSpec that holds the name of a named
 * EdDSA curve specification.
 * @author str4d
 *
 */
public class EdDSAGenParameterSpec implements AlgorithmParameterSpec {
    private final String name;

    public EdDSAGenParameterSpec(String stdName) {
        name = stdName;
    }

    public String getName() {
        return name;
    }
}
