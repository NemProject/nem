package org.nem.nis.service;

import org.nem.core.model.*;

public interface AccountIo {
	Account findByAddress(final Address address);

	SerializableList<Transaction> getAccountTransfers(final Address address);

	SerializableList<Block> getAccountBlocks(final Address address);
}
