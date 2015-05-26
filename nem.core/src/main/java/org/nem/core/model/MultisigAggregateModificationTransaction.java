package org.nem.core.model;

import org.nem.core.model.observers.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A transaction that describes the addition or removal of one or more cosignatories to or from
 * a multisig account.
 * <br>
 * First such transaction converts account to multisig account.
 */
public class MultisigAggregateModificationTransaction extends Transaction {
	private final List<MultisigCosignatoryModification> modifications;

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
			final Collection<MultisigCosignatoryModification> modifications) {
		super(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, 1, timeStamp, sender);

		validateModifications(modifications);
		this.modifications = new ArrayList<>(modifications);
		Collections.sort(this.modifications);
	}

	private static void validateModifications(final Collection<MultisigCosignatoryModification> modifications) {
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
		this.modifications = deserializer.readObjectArray("modifications", MultisigCosignatoryModification::new);

		validateModifications(this.modifications);
		Collections.sort(this.modifications);
	}

	/**
	 * Gets the modifications.
	 *
	 * @return The modifications.
	 */
	public Collection<MultisigCosignatoryModification> getModifications() {
		return Collections.unmodifiableCollection(this.modifications);
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObjectArray("modifications", this.modifications);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		for (final MultisigCosignatoryModification modification : this.modifications) {
			observer.notify(new AccountNotification(modification.getCosignatory()));
			observer.notify(new MultisigModificationNotification(this.getSigner(), modification));
		}

		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getSigner(), this.getFee()));
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return this.getModifications().stream().map(MultisigCosignatoryModification::getCosignatory).collect(Collectors.toList());
	}
}
