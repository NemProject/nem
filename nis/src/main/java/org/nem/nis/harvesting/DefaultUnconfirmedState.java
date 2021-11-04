package org.nem.nis.harvesting;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.AggregateSingleTransactionValidatorBuilder;
import org.nem.nis.validators.unconfirmed.TransactionDeadlineValidator;
import org.nem.nis.websocket.UnconfirmedTransactionListener;
import org.nem.nis.ForkConfiguration;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * A default implementation of UnconfirmedState.
 */
public class DefaultUnconfirmedState implements UnconfirmedState {
	private static final Logger LOGGER = Logger.getLogger(DefaultUnconfirmedState.class.getName());

	private final UnconfirmedTransactionsCache transactions;
	private final BlockTransactionObserver transferObserver;
	private final TransactionSpamFilter spamFilter;
	private final ReadOnlyNisCache nisCache;
	private final Supplier<BlockHeight> blockHeightSupplier;
	private final ForkConfiguration forkConfiguration;
	private final SingleTransactionValidator singleValidator;
	private final BatchTransactionValidator batchValidator;
	private final Supplier<BlockNotificationContext> notificationContextSupplier;
	private final ArrayList<UnconfirmedTransactionListener> listeners;

	/**
	 * Creates a default unconfirmed state.
	 *
	 * @param transactions The unconfirmed transactions cache.
	 * @param validatorFactory The validator factory.
	 * @param transferObserver The transfer observer.
	 * @param spamFilter The spam filter.
	 * @param nisCache The (unconfirmed) nis cache.
	 * @param timeProvider The time provider.
	 * @param blockHeightSupplier The block height supplier.
	 * @param forkConfiguration The fork configuration.
	 */
	public DefaultUnconfirmedState(final UnconfirmedTransactionsCache transactions, final TransactionValidatorFactory validatorFactory,
			final BlockTransactionObserver transferObserver, final TransactionSpamFilter spamFilter, final ReadOnlyNisCache nisCache,
			final TimeProvider timeProvider, final Supplier<BlockHeight> blockHeightSupplier, final ForkConfiguration forkConfiguration) {
		this.transactions = transactions;
		this.transferObserver = transferObserver;
		this.spamFilter = spamFilter;
		this.nisCache = nisCache;
		this.blockHeightSupplier = blockHeightSupplier;
		this.forkConfiguration = forkConfiguration;

		final AggregateSingleTransactionValidatorBuilder singleValidatorBuilder = validatorFactory.createIncompleteSingleBuilder(nisCache);
		singleValidatorBuilder.add(new TransactionDeadlineValidator(timeProvider));
		this.singleValidator = singleValidatorBuilder.build();

		this.batchValidator = validatorFactory.createBatch(nisCache.getTransactionHashCache());

		this.notificationContextSupplier = () -> new BlockNotificationContext(blockHeightSupplier.get(), timeProvider.getCurrentTime(),
				NotificationTrigger.Execute);

		this.listeners = new ArrayList<>();
	}

	@Override
	public Amount getUnconfirmedBalance(final Account account) {
		return this.nisCache.getAccountStateCache().findStateByAddress(account.getAddress()).getAccountInfo().getBalance();
	}

	@Override
	public Quantity getUnconfirmedMosaicBalance(final Account account, final MosaicId mosaicId) {
		return this.nisCache.getNamespaceCache().get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId).getBalances()
				.getBalance(account.getAddress());
	}

	@Override
	public ValidationResult addNewBatch(final Collection<Transaction> transactions) {
		final Collection<Transaction> filteredTransactions = this.spamFilter.filter(transactions);
		if (filteredTransactions.isEmpty()) {
			return ValidationResult.FAILURE_TRANSACTION_CACHE_TOO_FULL;
		}

		final ValidationResult transactionValidationResult = this.validateBatch(filteredTransactions);
		if (!transactionValidationResult.isSuccess()) {
			return transactionValidationResult;
		}

		return ValidationResult.aggregateNoShortCircuit(filteredTransactions.stream().map(this::add).iterator());
	}

	@Override
	public ValidationResult addNew(final Transaction transaction) {
		// check is needed to distinguish between NEUTRAL and FAILURE_TRANSACTION_CACHE_TOO_FULL
		if (this.transactions.contains(transaction)) {
			return ValidationResult.NEUTRAL;
		}

		final Collection<Transaction> filteredTransactions = this.spamFilter.filter(Collections.singletonList(transaction));
		if (filteredTransactions.isEmpty()) {
			return ValidationResult.FAILURE_TRANSACTION_CACHE_TOO_FULL;
		}

		final ValidationResult transactionValidationResult = this.validateBatch(filteredTransactions);
		return transactionValidationResult.isSuccess() ? this.add(transaction) : transactionValidationResult;
	}

	@Override
	public ValidationResult addExisting(final Transaction transaction) {
		return this.add(transaction);
	}

	@Override
	public void addListener(final UnconfirmedTransactionListener transactionListener) {
		this.listeners.add(transactionListener);
	}

	@Override
	public List<UnconfirmedTransactionListener> getListeners() {
		return this.listeners;
	}

	private ValidationResult verifyAndValidate(final Transaction transaction) {
		if (!transaction.verify()) {
			final Hash transactionHash = HashUtils.calculateHash(transaction);
			if (!this.forkConfiguration.getTreasuryReissuanceForkTransactionHashes().contains(transactionHash)) {
				return ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE;
			}

			LOGGER.info(String.format("Treasury reissuance transaction %s allowed", transactionHash));
		}

		final ValidationResult validationResult = this.validateSingle(transaction);
		if (!validationResult.isSuccess()) {
			LOGGER.warning(String.format("Transaction from %s rejected (%s)", transaction.getSigner().getAddress(), validationResult));
			return validationResult;
		}

		return ValidationResult.SUCCESS;
	}

	private ValidationResult add(final Transaction transaction) {
		ValidationResult validationResult = this.verifyAndValidate(transaction);
		if (!validationResult.isSuccess()) {
			return validationResult;
		}

		validationResult = this.transactions.add(transaction);
		if (!validationResult.isSuccess()) {
			return validationResult;
		}

		final ValidationResult finalValidationResult = validationResult;
		listeners.stream().forEach(l -> l.pushTransaction(transaction, finalValidationResult));

		this.execute(transaction);
		return validationResult;
	}

	private void execute(final Transaction transaction) {
		transaction.execute(
				new BlockTransactionObserverToTransactionObserverAdapter(this.transferObserver, this.notificationContextSupplier.get()),
				NisCacheUtils.createTransactionExecutionState(this.nisCache));
	}

	private ValidationResult validateBatch(final Collection<Transaction> transactions) {
		final TransactionsContextPair pair = new TransactionsContextPair(transactions, this.createValidationContext());
		return this.batchValidator.validate(Collections.singletonList(pair));
	}

	private ValidationResult validateSingle(final Transaction transaction) {
		return this.singleValidator.validate(transaction, this.createValidationContext());
	}

	private ValidationContext createValidationContext() {
		final BlockHeight currentHeight = this.blockHeightSupplier.get();
		final ValidationState validationState = NisCacheUtils.createValidationState(this.nisCache);
		return new ValidationContext(currentHeight.next(), currentHeight, validationState);
	}
}
