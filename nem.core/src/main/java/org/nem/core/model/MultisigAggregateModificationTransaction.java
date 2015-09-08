package org.nem.core.model;

import org.nem.core.model.observers.*;
import org.nem.core.model.transactions.extensions.*;
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
	private static final int MIN_MODIFICATION_VERSION = 2;
	private static final int CURRENT_VERSION = MIN_MODIFICATION_VERSION;
	private final List<MultisigCosignatoryModification> cosignatoryModifications;
	private final MultisigMinCosignatoriesModification minCosignatoriesModification;

	//region VALIDATION_EXTENSIONS

	private static final AggregateTransactionValidationExtension<MultisigAggregateModificationTransaction> VALIDATION_EXTENSIONS = new AggregateTransactionValidationExtension<>(
			Arrays.asList(
					new TransactionValidationExtension<MultisigAggregateModificationTransaction>() {
						@Override
						public boolean isApplicable(final int version) {
							return true;
						}

						@Override
						public void validate(final MultisigAggregateModificationTransaction transaction) {
							if (null == transaction.cosignatoryModifications) {
								throw new IllegalArgumentException("cosignatory modifications cannot be null");
							}
						}
					},
					new TransactionValidationExtension<MultisigAggregateModificationTransaction>() {
						@Override
						public boolean isApplicable(final int version) {
							return version < MIN_MODIFICATION_VERSION;
						}

						@Override
						public void validate(final MultisigAggregateModificationTransaction transaction) {
							if (transaction.cosignatoryModifications.isEmpty()) {
								throw new IllegalArgumentException("Cosignatory modifications cannot be empty");
							}

							if (null != transaction.minCosignatoriesModification) {
								final String message = String.format(
										"min cosignatory modification cannot be attached to transaction with version %d",
										transaction.getEntityVersion());
								throw new IllegalArgumentException(message);
							}
						}
					},
					new TransactionValidationExtension<MultisigAggregateModificationTransaction>() {
						@Override
						public boolean isApplicable(final int version) {
							return version >= MIN_MODIFICATION_VERSION;
						}

						@Override
						public void validate(final MultisigAggregateModificationTransaction transaction) {
							if (transaction.cosignatoryModifications.isEmpty() && null == transaction.minCosignatoriesModification) {
								throw new IllegalArgumentException("Either cosignatory modifications or change of minimum cosignatories must be present");
							}
						}
					}
			));

	//endregion

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
		this.cosignatoryModifications = null == cosignatoryModifications ? null : new ArrayList<>(cosignatoryModifications);
		this.minCosignatoriesModification = minCosignatoriesModification;

		VALIDATION_EXTENSIONS.validate(this);
		assert this.cosignatoryModifications != null;
		Collections.sort(this.cosignatoryModifications);
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
		if (this.getEntityVersion() >= CURRENT_VERSION) {
			this.minCosignatoriesModification = deserializer.readOptionalObject("minCosignatories", MultisigMinCosignatoriesModification::new);
		} else {
			this.minCosignatoriesModification = null;
		}

		VALIDATION_EXTENSIONS.validate(this);
		Collections.sort(this.cosignatoryModifications);
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

		if (this.getEntityVersion() >= CURRENT_VERSION) {
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

		super.transfer(observer);
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return this.getCosignatoryModifications().stream().map(MultisigCosignatoryModification::getCosignatory).collect(Collectors.toList());
	}
}
