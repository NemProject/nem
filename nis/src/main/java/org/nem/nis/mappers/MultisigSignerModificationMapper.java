package org.nem.nis.mappers;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.MultisigModification;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;

import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.MultisigTransaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

		final MultisigSignerModification dbMultisigSignerModification = new MultisigSignerModification();
		final org.nem.nis.dbmodel.Account sender = accountDaoLookup.findByAddress(multisigSignerModification.getSigner().getAddress());

		// TODO: move it to MultisigModificationMapper
		final Set<org.nem.nis.dbmodel.MultisigModification> multisigModifications = new HashSet<>(multisigSignerModification.getModifications().size());
		for (final MultisigModification multisigModification : multisigSignerModification.getModifications()) {
			final org.nem.nis.dbmodel.Account remote = accountDaoLookup.findByAddress(multisigModification.getCosignatory().getAddress());
			final org.nem.nis.dbmodel.MultisigModification dbModification = new org.nem.nis.dbmodel.MultisigModification();
			dbModification.setCosignatory(remote);
			dbModification.setModificationType(multisigModification.getModificationType().value());

			dbModification.setMultisigSignerModification(dbMultisigSignerModification);
			multisigModifications.add(dbModification);
		}

		AbstractTransferMapper.toDbModel(multisigSignerModification, sender, blockIndex, orderIndex, dbMultisigSignerModification);
		dbMultisigSignerModification.setMultisigModifications(multisigModifications);

		return dbMultisigSignerModification;
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

		final List<MultisigModification> multisigModifications = new ArrayList<>(dbMultisig.getMultisigModifications().size());
		for (final org.nem.nis.dbmodel.MultisigModification multisigModification : dbMultisig.getMultisigModifications()) {
			final Address cosignatoryAddress = AccountToAddressMapper.toAddress(multisigModification.getCosignatory());
			final Account cosignatory = accountLookup.findByAddress(cosignatoryAddress);

			multisigModifications.add(new MultisigModification(
					MultisigModificationType.fromValueOrDefault(multisigModification.getModificationType()),
					cosignatory
			));
		}
		final MultisigSignerModificationTransaction transfer = new MultisigSignerModificationTransaction(
				new TimeInstant(dbMultisig.getTimeStamp()),
				sender,
				multisigModifications);

		transfer.setFee(new Amount(dbMultisig.getFee()));
		transfer.setDeadline(new TimeInstant(dbMultisig.getDeadline()));
		transfer.setSignature(new Signature(dbMultisig.getSenderProof()));
		return transfer;
	}
}
