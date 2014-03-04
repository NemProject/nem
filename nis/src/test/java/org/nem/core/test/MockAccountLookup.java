package org.nem.core.test;

import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;

import java.util.HashMap;

/**
 * A mock AccountLookup implementation.
 */
public class MockAccountLookup implements AccountLookup {

    private final boolean shouldReturnNulls;
    private int numFindByIdCalls;
    private HashMap<Address, Account> accountMap = new HashMap<>();

    /**
     * Creates a new mock account lookup that never returns nulls.
     */
    public MockAccountLookup() {
        this(false);
    }

    /**
     * Creates a new mock account lookup that can optionally return null accounts
     * instead of mock accounts.
     *
     * @param shouldReturnNulls true if the default return value for an unknown account should be null.
     */
    public MockAccountLookup(final boolean shouldReturnNulls) {
        this.shouldReturnNulls = shouldReturnNulls;
    }

    @Override
    public Account findByAddress(final Address id) {
        ++this.numFindByIdCalls;

        final Account account = this.accountMap.get(id);
        if (null != account)
            return account;

        return this.shouldReturnNulls ? null : new MockAccount(id);
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