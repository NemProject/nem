package org.nem.core.test;

import org.nem.core.model.Account;
import org.nem.core.serialization.AccountLookup;

public class MockAccountLookup implements AccountLookup {

    private int numFindByIdCalls;

    @Override
    public Account findById(final String id) throws Exception {
        ++this.numFindByIdCalls;
        return new MockAccount(id);
    }

    public int getNumFindByIdCalls() { return this.numFindByIdCalls; }
}