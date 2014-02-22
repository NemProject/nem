package org.nem.core.test;

import org.nem.core.model.Address;

/**
 * A mock Address implementation.
 */
public class MockAddress extends Address {
    private final String encoded;

    /**
     * Creates a mock address.
     *
     * @param encoded The desired encoded address.
     */
    public MockAddress(final String encoded) {
        super((byte) 0x68, "Lorem ipsum dolor sit amet, consectetur adipisicing elit".getBytes());
        this.encoded = encoded;
    }

    @Override
    public String getEncoded() {
        return encoded;
    }
}
