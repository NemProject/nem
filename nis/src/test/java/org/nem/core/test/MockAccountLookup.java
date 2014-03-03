package org.nem.core.test;

import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;

import java.util.HashMap;

/**
 * A mock AccountLookup implementation.
 */
public class MockAccountLookup implements AccountLookup {

    private int numFindByIdCalls;
    private HashMap<Address, Account> accountMap = new HashMap<>();

    @Override
    public Account findByAddress(final Address id) {
        ++this.numFindByIdCalls;

        final Account account = this.accountMap.get(id);
        return null == account ? new MockAccount(id) : account;
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
    public void setMockAccount(final Account account) {
        this.accountMap.put(account.getAddress(), account);
    }
}