package org.nem.nis.test;

import org.nem.core.model.*;
import org.nem.core.model.MultisigModification;
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
		return new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
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
	 * TODO 20140106 J-G: please rename
	 * Creates a multisig signer modification.
	 *
	 * @return The multisig signer modification.
	 */
	public static MultisigAggregateModificationTransaction createSignerModification() {
		return new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, Utils.generateRandomAccount())));
	}
}
