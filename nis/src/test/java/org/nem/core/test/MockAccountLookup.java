package org.nem.core.test;

import org.nem.core.model.Address;
import org.nem.core.model.Account;
import org.nem.core.serialization.AccountLookup;

/**
 * A mock AccountLookup implementation.
 */
public class MockAccountLookup implements AccountLookup {

    private int numFindByIdCalls;
    private Account mockAccount;

    @Override
    public Account findByAddress(final Address id) {
        ++this.numFindByIdCalls;
        return null == this.mockAccount ? new MockAccount(id) : this.mockAccount;
    }

    /**
     * Returns the number of times findByAddress was called.
     *
     * @return The number of times findByAddress was called.
     */
    public int getNumFindByIdCalls() { return this.numFindByIdCalls; }

    /**
     * Sets the account that should be returned by findByAddress.
     *
     * @param account The account that should be returned by findByAddress.
     */
    public void setMockAccount(final Account account) { this.mockAccount = account; }
}