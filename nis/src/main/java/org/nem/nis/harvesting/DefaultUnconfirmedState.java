package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.AggregateSingleTransactionValidatorBuilder;
import org.nem.nis.validators.unconfirmed.TransactionDeadlineValidator;

import java.util.*;
import java.util.function.*;
import java.util.logging.Logger;

/**
 * A default implementation of UnconfirmedState.
 */
public class DefaultUnconfirmedState implements UnconfirmedState {
	private static final Logger LOGGER = Logger.getLogger(DefaultUnconfirmedState.class.getName());

	private final UnconfirmedTransactionsCache transactions;
	private final UnconfirmedBalancesObserver unconfirmedBalances;
	private final UnconfirmedMosaicBalancesObserver unconfirmedMosaicBalances;
	private final TransactionObserver transferObserver;
	private final TransactionSpamFilter spamFilter;
	private final Supplier<BlockHeight> blockHeightSupplier;
	private final SingleTransactionValidator singleValidator;
	private final BatchTransactionValidator batchValidator;

	/**
	 * Creates a default unconfirmed state.
	 *
	 * @param transactions The unconfirmed transactions cache.
	 * @param unconfirmedBalances The unconfirmed balances.
	 * @param unconfirmedMosaicBalances The unconfirmed mosaic balances.
	 * @param validatorFactory The validator factory.
	 * @param transferObserver The transfer observer.
	 * @param spamFilter The spam filter.
	 * @param nisCache The (unconfirmed) nis cache.
	 * @param timeProvider The time provider.
	 * @param blockHeightSupplier The block height supplier.
	 */
	public DefaultUnconfirmedState(
			final UnconfirmedTransactionsCache transactions,
			final UnconfirmedBalancesObserver unconfirmedBalances,
			final UnconfirmedMosaicBalancesObserver unconfirmedMosaicBalances,
			final TransactionValidatorFactory validatorFactory,
			final TransactionObserver transferObserver,
			final TransactionSpamFilter spamFilter,
			final ReadOnlyNisCache nisCache,
			final TimeProvider timeProvider,
			final Supplier<BlockHeight> blockHeightSupplier) {
		this.transactions = transactions;
		this.unconfirmedBalances = unconfirmedBalances;
		this.unconfirmedMosaicBalances = unconfirmedMosaicBalances;
		this.transferObserver = transferObserver;
		this.spamFilter = spamFilter;
		this.blockHeightSupplier = blockHeightSupplier;

		final AggregateSingleTransactionValidatorBuilder singleValidatorBuilder = validatorFactory.createIncompleteSingleBuilder(nisCache);
		singleValidatorBuilder.add(new TransactionDeadlineValidator(timeProvider));
		this.singleValidator = singleValidatorBuilder.build();

		this.batchValidator = validatorFactory.createBatch(nisCache.getTransactionHashCache());
	}

	@Override
	public Amount getUnconfirmedBalance(final Account account) {
		return this.unconfirmedBalances.get(account);
	}

	@Override
	public Quantity getUnconfirmedMosaicBalance(final Account account, final MosaicId mosaicId) {
		return this.unconfirmedMosaicBalances.get(account, mosaicId);
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
		return transactionValidationResult.isSuccess()
				? this.add(transaction)
				: transactionValidationResult;
	}

	@Override
	public ValidationResult addExisting(final Transaction transaction) {
		return this.add(transaction);
	}

	private ValidationResult verifyAndValidate(final Transaction transaction) {
		if (!transaction.verify()) {
			return ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE;
		}

		final ValidationResult validationResult = this.validateSingle(transaction);
		if (!validationResult.isSuccess()) {
			LOGGER.warning(String.format("Transaction from %s rejected (%s)", transaction.getSigner().getAddress(), validationResult));
			return validationResult;
		}

		return ValidationResult.SUCCESS;
	}

	private ValidationResult add(final Transaction transaction) {
		final List<Function<Transaction, ValidationResult>> validators = new ArrayList<>();
		validators.add(this::verifyAndValidate);
		validators.add(this.transactions::add);

		final ValidationResult validationResult = ValidationResult.aggregate(validators.stream().map(v -> v.apply(transaction)).iterator());
		if (validationResult.isSuccess()) {
			transaction.execute(this.transferObserver);
		}

		return validationResult;
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
		final ValidationState validationState = new ValidationState(
				(account, amount) -> this.getUnconfirmedBalance(account).compareTo(amount) >= 0,
				(account, mosaic) -> this.getUnconfirmedMosaicBalance(account, mosaic.getMosaicId()).compareTo(mosaic.getQuantity()) >= 0);
		return new ValidationContext(
				currentHeight.next(),
				currentHeight,
				validationState);
	}
}
