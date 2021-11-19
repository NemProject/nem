package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.validators.transaction.*;
import org.nem.nis.validators.unconfirmed.*;
import org.nem.nis.ForkConfiguration;

/**
 * Factory for creating TransactionValidator objects.
 */
public class TransactionValidatorFactory {
	private final TimeProvider timeProvider;
	private final NetworkInfo networkInfo;
	private final ForkConfiguration forkConfiguration;
	private final boolean ignoreFees;

	/**
	 * Creates a new factory.
	 *
	 * @param timeProvider The time provider.
	 * @param networkInfo The network info.
	 * @param forkConfiguration The fork configuration.
	 * @param ignoreFees True to ignore fees.
	 */
	public TransactionValidatorFactory(final TimeProvider timeProvider, final NetworkInfo networkInfo,
			final ForkConfiguration forkConfiguration, final boolean ignoreFees) {
		this.timeProvider = timeProvider;
		this.networkInfo = networkInfo;
		this.forkConfiguration = forkConfiguration;
		this.ignoreFees = ignoreFees;
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
	 * Creates a transaction validator builder that is only initialized with single validators and can be used for verifying blocks.
	 *
	 * @param nisCache The nis cache.
	 * @return The builder.
	 */
	public AggregateSingleTransactionValidatorBuilder createSingleBuilder(final ReadOnlyNisCache nisCache) {
		final AggregateSingleTransactionValidatorBuilder builder = this.createIncompleteSingleBuilder(nisCache);
		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.MULTISIG,
				new MultisigSignaturesPresentValidator(nisCache.getAccountStateCache())));
		return builder;
	}

	/**
	 * Creates a transaction validator builder that is only initialized with single validators and should be used for verifying transactions
	 * outside of blocks (it excludes validators that check for "incomplete" transactions).
	 *
	 * @param nisCache The nis cache.
	 * @return The builder.
	 */
	public AggregateSingleTransactionValidatorBuilder createIncompleteSingleBuilder(final ReadOnlyNisCache nisCache) {
		final ReadOnlyAccountStateCache accountStateCache = nisCache.getAccountStateCache();
		final ReadOnlyNamespaceCache namespaceCache = nisCache.getNamespaceCache();
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();

		builder.add(new DeadlineValidator());
		builder.add(new MinimumFeeValidator(this.networkInfo, nisCache.getNamespaceCache(), ignoreFees));
		builder.add(new VersionTransactionValidator());
		builder.add(new TransactionNonFutureEntityValidator(this.timeProvider));
		builder.add(new NemesisSinkValidator(this.forkConfiguration.getTreasuryReissuanceForkHeight()));
		builder.add(new BalanceValidator());
		builder.add(new TransactionNetworkValidator());

		builder.add(new RemoteNonOperationalValidator(accountStateCache));
		builder.add(new MultisigNonOperationalValidator(this.forkConfiguration, accountStateCache));

		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.TRANSFER, new TransferTransactionValidator()));

		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.IMPORTANCE_TRANSFER,
				new ImportanceTransferTransactionValidator(accountStateCache, namespaceCache)));

		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.MULTISIG,
				new MultisigTransactionSignerValidator(accountStateCache)));
		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.MULTISIG,
				new FeeSinkNonOperationalValidator(this.forkConfiguration)));

		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
				new MultisigCosignatoryModificationValidator(accountStateCache)));
		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
				new NumCosignatoryRangeValidator(accountStateCache)));

		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.PROVISION_NAMESPACE,
				new ProvisionNamespaceTransactionValidator(namespaceCache)));

		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.MOSAIC_DEFINITION_CREATION,
				new MosaicDefinitionCreationTransactionValidator(namespaceCache)));

		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.MOSAIC_SUPPLY_CHANGE,
				new MosaicSupplyChangeTransactionValidator(namespaceCache)));

		builder.add(new TSingleTransactionValidatorAdapter<>(TransactionTypes.TRANSFER, new MosaicBagValidator(namespaceCache)));

		builder.add(new MosaicBalanceValidator());

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
