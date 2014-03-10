package org.nem.nis;


import org.nem.core.model.ByteArray;
import org.nem.core.model.HashUtils;
import org.nem.core.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class BlockChain {
	private ConcurrentMap<ByteArray, Transaction> unconfirmedTransactions;

	public BlockChain() {
		unconfirmedTransactions = new ConcurrentHashMap<>();
	}

	/**
	 *
	 * @param transaction - transaction that isValid() and verify()-ed
	 * @return
	 */
	public boolean processTransaction(Transaction transaction) {

		int currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		// rest is checked by isValid()
		if (transaction.getTimestamp() > currentTime + 30) {
			return false;
		}

		ByteArray transactionHash = new ByteArray(HashUtils.calculateHash(transaction));

		synchronized (BlockChain.class) {
			// TODO: check if transaction isn't already in DB
		}

		Transaction swapTest = unconfirmedTransactions.putIfAbsent(transactionHash, transaction);
		if (swapTest != null) {
			return false;
		}

		return true;
	}
}
