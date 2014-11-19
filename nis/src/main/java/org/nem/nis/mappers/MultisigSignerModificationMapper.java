package org.nem.nis.mappers;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

/**
 * Static class that contains functions for converting to and from
 * db-model MultisigSignerModification
 * and model MultisigSignerModificationTransaction.
 */
public class MultisigSignerModificationMapper {

	/**
	 * Converts MultisigSignerModificationTransaction model to MultisigSignerModification db-model
	 *
	 * @param multisigSignerModification The multisig signer modification transaction model.
	 * @param blockIndex The index of the transfer within the owning block.
	 * @param orderIndex The index of the transfer within the owning block's collection of similar transactions.
	 * @param accountDaoLookup The account dao lookup object.
	 *
	 * @return The MultisigSignerModification db-model.
	 */
	public static MultisigSignerModification toDbModel(
			final MultisigSignerModificationTransaction multisigSignerModification,
			final int blockIndex,
			final int orderIndex,
			final AccountDaoLookup accountDaoLookup) {
		final org.nem.nis.dbmodel.Account sender = accountDaoLookup.findByAddress(multisigSignerModification.getSigner().getAddress());
		final org.nem.nis.dbmodel.Account remote = accountDaoLookup.findByAddress(multisigSignerModification.getCosignatory().getAddress());

		final Hash txHash = HashUtils.calculateHash(multisigSignerModification);
		return new MultisigSignerModification(
				txHash,
				multisigSignerModification.getVersion(),
				multisigSignerModification.getType(),
				multisigSignerModification.getFee().getNumMicroNem(),
				multisigSignerModification.getTimeStamp().getRawTime(),
				multisigSignerModification.getDeadline().getRawTime(),
				sender,
				// proof
				multisigSignerModification.getSignature().getBytes(),
				remote,
				multisigSignerModification.getModificationType().value(),
				orderIndex,
				blockIndex, // index
				0L);
	}

	/**
	 * Converts a MultisigSignerModification db-model to a MultisigSignerModificationTransaction model.
	 *
	 * @param dbMultisig The importance transfer db-model.
	 * @param accountLookup The account lookup object.
	 *
	 * @return The MultisigSignerModificationTransaction model.
	 */
	public static MultisigSignerModificationTransaction toModel(final MultisigSignerModification dbMultisig, final AccountLookup accountLookup) {
		final Address senderAccount = AccountToAddressMapper.toAddress(dbMultisig.getSender());
		final Account sender = accountLookup.findByAddress(senderAccount);

		final Address cosignatoryAddress = AccountToAddressMapper.toAddress(dbMultisig.getCosignatory());
		final Account cosignatory = accountLookup.findByAddress(cosignatoryAddress);

		final MultisigSignerModificationTransaction transfer = new MultisigSignerModificationTransaction(
				new TimeInstant(dbMultisig.getTimeStamp()),
				sender,
				MultisigSignerModificationTransaction.ModificationType.fromValueOrDefault(dbMultisig.getModificationType()),
				cosignatory);

		transfer.setFee(new Amount(dbMultisig.getFee()));
		transfer.setDeadline(new TimeInstant(dbMultisig.getDeadline()));
		transfer.setSignature(new Signature(dbMultisig.getSenderProof()));
		return transfer;
	}
}
