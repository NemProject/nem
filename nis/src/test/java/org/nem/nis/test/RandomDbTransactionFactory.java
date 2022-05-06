package org.nem.nis.test;

import org.nem.core.model.Address;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

import java.util.HashSet;

/**
 * Factory class used to create random (concrete) db transactions.
 */
public class RandomDbTransactionFactory {

	/**
	 * Creates a transfer transaction.
	 *
	 * @return The transfer.
	 */
	public static DbTransferTransaction createTransfer() {
		return createTransferWithTimeStamp(0);
	}

	/**
	 * Creates a transfer transaction.
	 *
	 * @param timeStamp the time stamp.
	 * @return The transfer.
	 */
	public static DbTransferTransaction createTransferWithTimeStamp(final int timeStamp) {
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final DbAccount account = new DbAccount(address);
		final DbTransferTransaction dbTransfer = new DbTransferTransaction();
		dbTransfer.setVersion(1);
		dbTransfer.setTransferHash(Utils.generateRandomHash());
		dbTransfer.setSender(account);
		dbTransfer.setSenderProof(Utils.generateRandomBytes(64));
		dbTransfer.setRecipient(account);
		dbTransfer.setTimeStamp(timeStamp);
		dbTransfer.setAmount(0L);
		dbTransfer.setFee(0L);
		dbTransfer.setDeadline(0);
		dbTransfer.setBlkIndex(0);
		return dbTransfer;
	}

	/**
	 * Creates a multisig transfer.
	 *
	 * @return The transfer.
	 */
	public static DbMultisigTransaction createMultisigTransfer() {
		return createMultisigTransferWithTimeStamp(0);
	}

	/**
	 * Creates a multisig transfer.
	 *
	 * @param timeStamp the time stamp.
	 * @return The transfer.
	 */
	public static DbMultisigTransaction createMultisigTransferWithTimeStamp(final int timeStamp) {
		final DbTransferTransaction dbTransfer = createTransferWithTimeStamp(timeStamp);
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final DbAccount account = new DbAccount(address);
		final DbMultisigTransaction dbMultisig = new DbMultisigTransaction();
		dbMultisig.setTransferTransaction(dbTransfer);
		dbMultisig.setMultisigSignatureTransactions(new HashSet<>());
		dbMultisig.setTransferHash(Utils.generateRandomHash());
		dbMultisig.setSender(account);
		dbMultisig.setSenderProof(Utils.generateRandomBytes(64));
		dbMultisig.setTimeStamp(timeStamp);
		dbMultisig.setFee(0L);
		dbMultisig.setDeadline(0);
		dbMultisig.setBlkIndex(0);
		return dbMultisig;
	}
}
