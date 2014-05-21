package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.TransactionMetaDataPair;

public interface AccountIo {
	Account findByAddress(final Address address);

	SerializableList<TransactionMetaDataPair> getAccountTransfers(Address address);

	SerializableList<Block> getAccountBlocks(final Address address);
}
