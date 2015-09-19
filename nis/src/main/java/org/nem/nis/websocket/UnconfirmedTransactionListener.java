package org.nem.nis.websocket;

import org.nem.core.model.Transaction;
import org.nem.core.model.ValidationResult;

public interface UnconfirmedTransactionListener {
	void pushTransaction(final Transaction peerChain, final ValidationResult validationResult);
}
