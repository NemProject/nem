package org.nem.core.test;

import org.nem.core.model.Address;

public class MockAddress extends Address {
    private final String encoded;

    public MockAddress(final String encoded) throws Exception {
        super((byte) 0x68, "Lorem ipsum dolor sit amet, consectetur adipisicing elit".getBytes());
        this.encoded = encoded;
    }

    @Override
    public String getEncoded() {
        return encoded;
    }
}
