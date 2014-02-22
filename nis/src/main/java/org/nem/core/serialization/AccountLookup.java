package org.nem.core.serialization;

import org.nem.core.model.Address;
import org.nem.core.model.Account;

public interface AccountLookup {
    public Account findByAddress(final Address id) throws Exception;
}
