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
	private static final int CURRENT_VERSION = 2;
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
	 * @param minCosignatoriesModification The change of the minimum number of cosignatories, is allowed to be null.
	 */
	public MultisigAggregateModificationTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Collection<MultisigCosignatoryModification> cosignatoryModifications,
			final MultisigMinCosignatoriesModification minCosignatoriesModification) {
		this(CURRENT_VERSION, timeStamp, sender, cosignatoryModifications, minCosignatoriesModification);
	}

	/**
	 * Creates a multisig aggregate modification transaction.
	 *
	 * @param version The transaction version.
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender (multisig account).
	 * @param cosignatoryModifications The list of cosignatory modifications.
	 * @param minCosignatoriesModification The change of the minimum number of cosignatories, is allowed to be null.
	 */
	public MultisigAggregateModificationTransaction(
			final int version,
			final TimeInstant timeStamp,
			final Account sender,
			final Collection<MultisigCosignatoryModification> cosignatoryModifications,
			final MultisigMinCosignatoriesModification minCosignatoriesModification) {
		super(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, version, timeStamp, sender);
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
		this.cosignatoryModifications = deserializer.readObjectArray("modifications", MultisigCosignatoryModification::new);
		if ((this.getVersion() & 0x00FFFFFF) >= CURRENT_VERSION) {
			this.minCosignatoriesModification = deserializer.readOptionalObject("minCosignatories", MultisigMinCosignatoriesModification::new);
		} else {
			this.minCosignatoriesModification = null;
		}

		validate(this.cosignatoryModifications, this.minCosignatoriesModification);
		Collections.sort(this.cosignatoryModifications);
	}

	private static void validate(
			final Collection<MultisigCosignatoryModification> cosignatoryModifications,
			final MultisigMinCosignatoriesModification minCosignatoriesModification) {
		if (null == cosignatoryModifications) {
			throw new IllegalArgumentException("cosignatory modifications cannot be null");
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
		serializer.writeObjectArray("modifications", this.cosignatoryModifications);

		if ((this.getVersion() & 0x00FFFFFF) >= CURRENT_VERSION) {
			serializer.writeObject("minCosignatories", this.minCosignatoriesModification);
		}
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
