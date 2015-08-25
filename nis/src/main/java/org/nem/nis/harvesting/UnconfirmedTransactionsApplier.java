package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.secret.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.AggregateSingleTransactionValidatorBuilder;
import org.nem.nis.validators.unconfirmed.TransactionDeadlineValidator;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class UnconfirmedTransactionsApplier {
	private static final Logger LOGGER = Logger.getLogger(UnconfirmedTransactionsApplier.class.getName());

	private final UnconfirmedTransactionsCache transactions;
	private final UnconfirmedBalancesObserver unconfirmedBalances;
	private final UnconfirmedMosaicBalancesObserver unconfirmedMosaicBalances;
	private final TransactionObserver transferObserver;
	private final TransactionValidatorFactory validatorFactory;
	private final SingleTransactionValidator singleValidator;
	private final TransactionSpamFilter spamFilter;
	private final ReadOnlyNisCache nisCache;
	private final TimeProvider timeProvider;
	private final Supplier<BlockHeight> blockHeightSupplier;

	public UnconfirmedTransactionsApplier(
			final UnconfirmedTransactionsCache transactions,
			final UnconfirmedBalancesObserver unconfirmedBalances,
			final UnconfirmedMosaicBalancesObserver unconfirmedMosaicBalances,
			final TransactionObserver transferObserver,
			final TransactionValidatorFactory validatorFactory,
			final SingleTransactionValidator singleValidator,
			final TransactionSpamFilter spamFilter,
			final ReadOnlyNisCache nisCache,
			final TimeProvider timeProvider,
			final Supplier<BlockHeight> blockHeightSupplier) {
		this.transactions = transactions;
		this.unconfirmedBalances = unconfirmedBalances;
		this.unconfirmedMosaicBalances = unconfirmedMosaicBalances;
		this.transferObserver = transferObserver;
		this.validatorFactory = validatorFactory;
		this.singleValidator = singleValidator;
		this.spamFilter = spamFilter;
		this.nisCache = nisCache;
		this.timeProvider = timeProvider;
		this.blockHeightSupplier = blockHeightSupplier;
	}

	public Amount getUnconfirmedBalance(final Account account) {
		return this.unconfirmedBalances.get(account);
	}

	public Quantity getUnconfirmedMosaicBalance(final Account account, final MosaicId mosaicId) {
		return this.unconfirmedMosaicBalances.get(account, mosaicId);
	}

	public ValidationResult addNewBatch(final Collection<Transaction> transactions) {
		final Collection<Transaction> filteredTransactions = this.spamFilter.filter(transactions);
		final ValidationResult transactionValidationResult = this.validateBatch(filteredTransactions);
		if (!transactionValidationResult.isSuccess()) {
			return transactionValidationResult;
		}

		return ValidationResult.aggregateNoShortCircuit(filteredTransactions.stream().map(this::add).iterator());
	}

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

	public ValidationResult addExisting(final Transaction transaction) {
		return this.add(transaction);
	}

	public ValidationResult verifyAndValidate(final Transaction transaction) {
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
		final ValidationResult validationResult = this.transactions.add(transaction);
		if (validationResult.isSuccess()) {
			transaction.execute(this.transferObserver);
		}

		return validationResult;
	}

	private ValidationResult validateBatch(final Collection<Transaction> transactions) {
		final TransactionsContextPair pair = new TransactionsContextPair(transactions, this.createValidationContext());
		return this.validatorFactory.createBatch(this.nisCache.getTransactionHashCache()).validate(Collections.singletonList(pair));
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

	private SingleTransactionValidator createSingleValidator() {
		final AggregateSingleTransactionValidatorBuilder builder = this.validatorFactory.createIncompleteSingleBuilder(this.nisCache);
		builder.add(new TransactionDeadlineValidator(this.timeProvider));
		return builder.build();
	}
}
