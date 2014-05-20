package org.nem.nis.service;

import org.nem.core.model.Account;
import org.nem.core.model.Address;
import org.nem.core.model.SerializableList;
import org.nem.core.model.Transaction;
import org.nem.core.model.ncc.TransactionMetaDataPair;

public interface AccountIo {
	Account findByAddress(Address address);

	SerializableList<TransactionMetaDataPair> getAccountTransfers(Address address);
}
