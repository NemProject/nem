package org.nem.core.test;

import org.nem.core.model.Address;
import org.nem.core.model.Account;
import org.nem.core.serialization.AccountLookup;

public class MockAccountLookup implements AccountLookup {

    private int numFindByIdCalls;
    private Account mockAccount;

    @Override
    public Account findByAddress(final Address id) throws Exception {
        ++this.numFindByIdCalls;
        return null == this.mockAccount ? new MockAccount(id) : this.mockAccount;
    }

    public int getNumFindByIdCalls() { return this.numFindByIdCalls; }

    public void setMockAccount(final Account account) { this.mockAccount = account; }
}