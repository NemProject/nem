package org.nem.core.serialization;

import org.nem.core.model.Account;

public interface AccountLookup {
    public Account findById(final String id) throws Exception;
}
