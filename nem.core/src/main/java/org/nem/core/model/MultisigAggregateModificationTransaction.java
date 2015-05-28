package org.nem.core.model;

import org.nem.core.model.observers.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A transaction that describes the addition or removal of one or more cosignatories to or from
 * a multisig account. Additionally the minimum number of cosignatories for a transaction can be set.
 * <br>
 * First such transaction converts an account to multisig account.
 */
public class MultisigAggregateModificationTransaction extends Transaction {
	private final List<MultisigCosignatoryModification> cosignatoryModifications;
	private final MultisigMinCosignatoriesModification minCosignatoriesModification;

	/**
	 * Creates a multisig aggregate modification transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender (multisig account).
	 * @param cosignatoryModifications The list of cosignatory modifications.
	 */
	public MultisigAggregateModificationTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Collection<MultisigCosignatoryModification> cosignatoryModifications) {
		this(timeStamp, sender, cosignatoryModifications, null);
	}

	/**
	 * Creates a multisig aggregate modification transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender (multisig account).
	 * @param cosignatoryModifications The list of cosignatory modifications.
	 * @param minCosignatoriesModification The minimum number of cosignatories, is allowed to be null.
	 */
	public MultisigAggregateModificationTransaction(
		final TimeInstant timeStamp,
			final Account sender,
			final Collection<MultisigCosignatoryModification> cosignatoryModifications,
			final MultisigMinCosignatoriesModification minCosignatoriesModification) {
		super(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, 1, timeStamp, sender);
		validate(cosignatoryModifications, minCosignatoriesModification);
		this.cosignatoryModifications = new ArrayList<>(cosignatoryModifications);
		Collections.sort(this.cosignatoryModifications);
		this.minCosignatoriesModification = minCosignatoriesModification;
	}

	/**
	 * Deserializes a multisig signer modification transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MultisigAggregateModificationTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, options, deserializer);
		this.cosignatoryModifications = deserializer.readObjectArray("cosignatoryModifications", MultisigCosignatoryModification::new);
		this.minCosignatoriesModification = deserializer.readOptionalObject("minCosignatories", MultisigMinCosignatoriesModification::new);
		validate(this.cosignatoryModifications, this.minCosignatoriesModification);
		Collections.sort(this.cosignatoryModifications);
	}

	private static void validate(
			final Collection<MultisigCosignatoryModification> cosignatoryModifications,
			final MultisigMinCosignatoriesModification minCosignatoriesModification) {
		if (null == cosignatoryModifications) {
			throw new IllegalArgumentException("cosignatoryModifications cannot be null");
		}

		if (cosignatoryModifications.isEmpty() && null == minCosignatoriesModification) {
			throw new IllegalArgumentException("Either cosignatory modifications or change of minimum cosignatories must be present");
		}
	}

	/**
	 * Gets the cosignatory modifications.
	 *
	 * @return The cosignatory modifications.
	 */
	public Collection<MultisigCosignatoryModification> getCosignatoryModifications() {
		return Collections.unmodifiableCollection(this.cosignatoryModifications);
	}

	/**
	 * Gets the minimum cosignatories modification. This is allowed to be null.
	 *
	 * @return The minimum cosignatories modification.
	 */
	public MultisigMinCosignatoriesModification getMinCosignatoriesModification() {
		return this.minCosignatoriesModification;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObjectArray("cosignatoryModifications", this.cosignatoryModifications);
		serializer.writeObject("minCosignatories", this.minCosignatoriesModification);
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		for (final MultisigCosignatoryModification modification : this.cosignatoryModifications) {
			observer.notify(new AccountNotification(modification.getCosignatory()));
			observer.notify(new MultisigCosignatoryModificationNotification(this.getSigner(), modification));
		}

		if (null != this.minCosignatoriesModification) {
			observer.notify(new MultisigMinCosignatoriesModificationNotification(this.getSigner(), this.minCosignatoriesModification));
		}

		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, this.getSigner(), this.getFee()));
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return this.getCosignatoryModifications().stream().map(MultisigCosignatoryModification::getCosignatory).collect(Collectors.toList());
	}
}
