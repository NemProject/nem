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
 *
 * TODO 20150106 G-J: MultisigAggregateModificationTransaction CANNOT have single modification
 * > otherwise we'd have to allow multiple MultisigAggregateModificationTransaction coming from single
 * > account (per block), and that would complicate validators even further
 */
public class MultisigAggregateModificationTransaction extends Transaction {
	final Collection<MultisigModification> modifications;

	/**
	 * Creates an multisig signer modification transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender (multisig account).
	 * @param modifications The list of modifications.
	 */
	public MultisigAggregateModificationTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Collection<MultisigModification> modifications) {
		super(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, 1, timeStamp, sender);
		this.modifications = modifications;

		validateModifications(this.modifications);
	}

	// TODO 20150103 add validation tests; should you also validate after deserialization?
	private static void validateModifications(final Collection<MultisigModification> modifications) {
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
	public MultisigAggregateModificationTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, options, deserializer);

		this.modifications = deserializer.readObjectArray("modifications", obj -> new MultisigModification(obj));
	}

	/**
	 * Gets the modifications.
	 * TODO 20150103 J-G: might want to return something other than a list.
	 *
	 * @return The modifications.
	 */
	public List<MultisigModification> getModifications() {
		return new ArrayList<>(this.modifications);
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
			observer.notify(new MultisigModificationNotification(this.getSigner(), modification));
		}

		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getSigner(), this.getFee()));
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
