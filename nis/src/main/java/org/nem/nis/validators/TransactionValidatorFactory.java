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
	 * @param nisCache The nis cache.
	 * @return The validator.
	 */
	public SingleTransactionValidator createSingle(final ReadOnlyNisCache nisCache) {
		return this.createSingleBuilder(nisCache).build();
	}

	/**
	 * Creates a transaction validator builder that is only initialized with single validators and can be used
	 * for verifying blocks.
	 *
	 * @param nisCache The nis cache.
	 * @return The builder.
	 */
	public AggregateSingleTransactionValidatorBuilder createSingleBuilder(final ReadOnlyNisCache nisCache) {
		final AggregateSingleTransactionValidatorBuilder builder = this.createIncompleteSingleBuilder(nisCache);
		builder.add(
				new TSingleTransactionValidatorAdapter<>(
						TransactionTypes.MULTISIG,
						new MultisigSignaturesPresentValidator(nisCache.getAccountStateCache())));
		return builder;
	}

	/**
	 * Creates a transaction validator builder that is only initialized with single validators and should be used
	 * for verifying transactions outside of blocks (it excludes validators that check for "incomplete" transactions).
	 *
	 * @param nisCache The nis cache.
	 * @return The builder.
	 */
	public AggregateSingleTransactionValidatorBuilder createIncompleteSingleBuilder(final ReadOnlyNisCache nisCache) {
		final ReadOnlyAccountStateCache accountStateCache = nisCache.getAccountStateCache();
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

		// TODO 20150705 J-B: might make sense to add a few integration tests for these and/or the observer side-effects
		// TODO 20150711 BR -> J: remind me, which test class do the integration tests go into?
		// TODO 20150720 J-B: validation ones: AbstractTransactionValidationTest; state change ones: BlockChainHarvesterTest
		// > but we don't have many of the latter :/

		builder.add(
				new TSingleTransactionValidatorAdapter<>(
						TransactionTypes.PROVISION_NAMESPACE,
						new ProvisionNamespaceTransactionValidator(nisCache.getNamespaceCache())));

		builder.add(
				new TSingleTransactionValidatorAdapter<>(
						TransactionTypes.MOSAIC_DEFINITION_CREATION,
						new MosaicDefinitionCreationTransactionValidator(nisCache.getNamespaceCache())));

		builder.add(
				new TSingleTransactionValidatorAdapter<>(
						TransactionTypes.MOSAIC_SUPPLY_CHANGE,
						new MosaicSupplyChangeTransactionValidator(nisCache.getNamespaceCache())));

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
