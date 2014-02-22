package org.nem.core.test;

import org.nem.core.crypto.KeyPair;
import org.nem.core.model.Account;
import org.nem.core.model.Address;

public class MockAccount extends Account {
    private final Address address;

    public MockAccount(final Address address) throws Exception {
        super(new KeyPair());
        this.address = address;
    }

    @Override
    public Address getAddress() { return this.address; }
}