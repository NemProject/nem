package org.nem.nis.validators;

import org.nem.core.model.TransactionTypes;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.validators.transaction.*;
import org.nem.nis.validators.unconfirmed.*;

/**
 * Factory for creating TransactionValidator objects.
 */
public class TransactionValidatorFactory {
	private final TimeProvider timeProvider;

	/**
	 * Creates a new factory.
	 *
	 * @param timeProvider The time provider.
	 */
	public TransactionValidatorFactory(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	/**
	 * Creates a transaction validator that only contains single validators.
	 *
	 * @param accountStateCache The account state cache.
	 * @return The validator.
	 */
	public SingleTransactionValidator createSingle(final ReadOnlyAccountStateCache accountStateCache) {
		return this.createSingleBuilder(accountStateCache).build();
	}

	/**
	 * Creates a transaction validator builder that is only initialized with single validators and can be used
	 * for verifying blocks.
	 *
	 * @param accountStateCache The account state cache.
	 * @return The builder.
	 */
	public AggregateSingleTransactionValidatorBuilder createSingleBuilder(final ReadOnlyAccountStateCache accountStateCache) {
		final AggregateSingleTransactionValidatorBuilder builder = this.createIncompleteSingleBuilder(accountStateCache);
		builder.add(
				new TSingleTransactionValidatorAdapter<>(
						TransactionTypes.MULTISIG,
						new MultisigSignaturesPresentValidator(accountStateCache)));
		return builder;
	}

	/**
	 * Creates a transaction validator builder that is only initialized with single validators and should be used
	 * for verifying transactions outside of blocks (it excludes validators that check for "incomplete" transactions).
	 *
	 * @param accountStateCache The account state cache.
	 * @return The builder.
	 */
	public AggregateSingleTransactionValidatorBuilder createIncompleteSingleBuilder(final ReadOnlyAccountStateCache accountStateCache) {
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();

		builder.add(new UniversalTransactionValidator());
		builder.add(new VersionTransactionValidator());
		builder.add(new TransactionNonFutureEntityValidator(this.timeProvider));
		builder.add(new NemesisSinkValidator());
		builder.add(new BalanceValidator());
		builder.add(new TransactionNetworkValidator());

		builder.add(new RemoteNonOperationalValidator(accountStateCache));
		builder.add(new MultisigNonOperationalValidator(accountStateCache));

		builder.add(
				new TSingleTransactionValidatorAdapter<>(
						TransactionTypes.TRANSFER,
						new TransferTransactionValidator()));

		builder.add(
				new TSingleTransactionValidatorAdapter<>(
						TransactionTypes.IMPORTANCE_TRANSFER,
						new ImportanceTransferTransactionValidator(accountStateCache)));

		builder.add(
				new TSingleTransactionValidatorAdapter<>(
						TransactionTypes.MULTISIG,
						new MultisigTransactionSignerValidator(accountStateCache)));

		builder.add(
				new TSingleTransactionValidatorAdapter<>(
						TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
						new MultisigCosignatoryModificationValidator(accountStateCache)));
		builder.add(
				new TSingleTransactionValidatorAdapter<>(
						TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
						new NumCosignatoryRangeValidator(accountStateCache)));
		return builder;
	}

	/**
	 * Creates a transaction validator that only contains batch validators.
	 *
	 * @param transactionHashCache The transaction hash cache.
	 * @return The validator.
	 */
	public BatchTransactionValidator createBatch(final ReadOnlyHashCache transactionHashCache) {
		final AggregateBatchTransactionValidatorBuilder builder = new AggregateBatchTransactionValidatorBuilder();
		builder.add(new BatchUniqueHashTransactionValidator(transactionHashCache));
		return builder.build();
	}
}
