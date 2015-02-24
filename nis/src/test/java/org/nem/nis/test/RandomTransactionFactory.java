package org.nem.nis.test;

import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import java.util.Arrays;

/**
 * Factory class used to create random (concrete) transactions.
 */
public class RandomTransactionFactory {

	/**
	 * Creates a transfer transaction.
	 *
	 * @return The transfer.
	 */
	public static TransferTransaction createTransfer() {
		return createTransfer(Utils.generateRandomAccount());
	}

	/**
	 * Creates a transfer transaction.
	 *
	 * @param signer The signer.
	 * @return The transfer.
	 */
	public static TransferTransaction createTransfer(final Account signer) {
		return new TransferTransaction(
				TimeInstant.ZERO,
				signer,
				Utils.generateRandomAccount(),
				Amount.fromNem(111),
				null);
	}

	/**
	 * Creates an importance transfer transaction.
	 *
	 * @return The importance transfer.
	 */
	public static ImportanceTransferTransaction createImportanceTransfer() {
		return new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				ImportanceTransferTransaction.Mode.Activate,
				Utils.generateRandomAccount());
	}

	/**
	 * Creates a multisig aggregate modification.
	 *
	 * @return The multisig aggregate modification.
	 */
	public static MultisigAggregateModificationTransaction createMultisigModification() {
		return new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, Utils.generateRandomAccount())));
	}

	/**
	 * Creates a multisig transfer.
	 *
	 * @param multisig The multisig account.
	 * @param cosigner The cosigner account.
	 * @return A multisig transfer.
	 */
	public static MultisigTransaction createMultisigTransfer(final Account multisig, final Account cosigner) {
		return new MultisigTransaction(
				TimeInstant.ZERO,
				cosigner,
				createTransfer(multisig));
	}
}
