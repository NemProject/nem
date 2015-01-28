package org.nem.core.model;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A transaction that describes the addition or removal of one or more cosignatories to or from
 * a multisig account.
 * <br/>
 * First such transaction converts account to multisig account.
 */
public class MultisigAggregateModificationTransaction extends Transaction {
	// TODO 20150127 J-B: can we change this back to a collection? just sort the list before assigning
	// > (i don't care about this private field, but for the public api i'd rather expose collection)
	private final List<MultisigModification> modifications;

	/**
	 * Creates a multisig aggregate modification transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender (multisig account).
	 * @param modifications The list of modifications.
	 */
	public MultisigAggregateModificationTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final List<MultisigModification> modifications) {
		super(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, 1, timeStamp, sender);
		this.modifications = modifications;

		validateModifications(this.modifications);
		Collections.sort(this.modifications);
	}

	private static void validateModifications(final Collection<MultisigModification> modifications) {
		if (null == modifications || modifications.isEmpty()) {
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

		validateModifications(this.modifications);
		Collections.sort(this.modifications);
	}

	/**
	 * Gets the modifications.
	 *
	 * @return The modifications.
	 */
	public List<MultisigModification> getModifications() {
		return Collections.unmodifiableList(this.modifications);
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
	public Amount getMinimumFee() {
		return Amount.fromNem(100 + this.getModifications().size() * 100);
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return this.getModifications().stream().map(m -> m.getCosignatory()).collect(Collectors.toList());
	}
}
