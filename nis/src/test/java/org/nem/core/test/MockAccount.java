package org.nem.core.test;

import org.nem.core.crypto.KeyPair;
import org.nem.core.model.Account;

public class MockAccount extends Account {
    private final String id;

    public MockAccount(final String id) throws Exception {
        super(new KeyPair());
        this.id = id;
    }

    @Override
    public String getId() { return this.id; }
}