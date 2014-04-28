package org.nem.nis.service;

import org.nem.core.model.Account;
import org.nem.core.model.Address;
import org.nem.core.model.SerializableList;
import org.nem.core.model.Transaction;

public interface AccountIo {
	Account findByAddress(Address address);

	SerializableList<Transaction> getAccountTransfers(Address address);
}
