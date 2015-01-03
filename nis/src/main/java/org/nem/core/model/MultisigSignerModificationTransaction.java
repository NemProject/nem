package org.nem.core.model;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A transaction which describes association of cosignatory with a multisig account.
 * <br/>
 * First such transaction converts account to multisig account.
 */
public class MultisigSignerModificationTransaction extends Transaction {
	final List<MultisigModification> modifications;

	/**
	 * Creates an multisig signer modification transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender (multisig account).
	 * @param modifications The list of modifications.
	 */
	public MultisigSignerModificationTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final List<MultisigModification> modifications) {
		super(TransactionTypes.MULTISIG_SIGNER_MODIFY, 1, timeStamp, sender);
		this.modifications = modifications;

		validateModifications(this.modifications);
	}

	// TODO 20150103 add validation tests; should you also validate after deserialization?
	private static void validateModifications(final List<MultisigModification> modifications) {
		if (modifications == null || modifications.isEmpty()) {
			throw new IllegalArgumentException("no modifications on the list");
		}
	}

	/**
	 * Deserializes a multisig signer modification transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MultisigSignerModificationTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MULTISIG_SIGNER_MODIFY, options, deserializer);

		this.modifications = deserializer.readObjectArray("modifications", obj -> new MultisigModification(obj));
	}

	/**
	 * Gets the modifications.
	 * TODO 20150103 J-G: might want to return something other than a list.
	 *
	 * @return The modifications.
	 */
	public List<MultisigModification> getModifications() {
		return this.modifications;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObjectArray("modifications", this.modifications);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		for (final MultisigModification modification : this.modifications) {
			observer.notify(new AccountNotification(modification.getCosignatory()));
		}

		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getSigner(), this.getFee()));
		observer.notify(new MultisigModificationNotification(this.getSigner(), this.modifications));
	}

	@Override
	protected Amount getMinimumFee() {
		// TODO 20141111 G-J,B: decide, but I believe this should be high
		return Amount.fromNem(1000);
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		// TODO 20141220 J-G: should review / test this; this should really have all of the accounts in the modifications i think
		return new ArrayList<>();
	}
}
