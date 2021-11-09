package org.nem.nis.mappers;

import org.nem.nis.dbmodel.*;

/**
 * Helper class containing functions to facilitate working with dbmodel objects.
 */
public class DbModelUtils {

	/**
	 * Gets the inner transaction from a multisig transaction.
	 *
	 * @param source The source multisig transaction.
	 * @return The inner transaction.
	 */
	@SuppressWarnings("rawtypes")
	public static AbstractBlockTransfer getInnerTransaction(final DbMultisigTransaction source) {
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			final AbstractBlockTransfer transaction = entry.getFromMultisig.apply(source);
			if (null != transaction) {
				return transaction;
			}
		}

		throw new IllegalArgumentException("dbmodel has invalid multisig transaction");
	}

	/**
	 * Gets a value indicating whether or not a transaction is an inner transaction.
	 *
	 * @param dbTransfer The transaction.
	 * @return true if the transaction is an inner transaction.
	 */
	public static boolean isInnerTransaction(final AbstractTransfer dbTransfer) {
		return null == dbTransfer.getSenderProof();
	}
}
