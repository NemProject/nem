package org.nem.nis.test;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.harvesting.*;
import org.nem.nis.ForkConfiguration;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.*;

public class UnconfirmedTransactionsTestUtils {
	public static final int CURRENT_TIME = 10_000;

	public interface UnconfirmedTransactionsTest {

		/**
		 * Creates the unconfirmed transactions cache.
		 *
		 * @param unconfirmedStateFactory The unconfirmed state factory to use.
		 * @param nisCache The NIS cache to use.
		 * @return The unconfirmed transactions cache.
		 */
		UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
				final ReadOnlyNisCache nisCache);
	}

	// region UnconfirmedTransactionsTestContext interface + implementations

	public interface UnconfirmedTransactionsTestContext {
		Account addAccount(final Amount amount);
	}

	public static class NonExecutingUnconfirmedTransactionsTestContext implements UnconfirmedTransactionsTestContext {
		private final ReadOnlyNisCache nisCache = NisCacheFactory.createReal();
		private final UnconfirmedTransactions transactions;

		public NonExecutingUnconfirmedTransactionsTestContext(
				final BiFunction<UnconfirmedStateFactory, ReadOnlyNisCache, UnconfirmedTransactions> creator) {
			final TimeProvider timeProvider = Utils.createMockTimeProvider(CURRENT_TIME);
			final UnconfirmedStateFactory factory = new UnconfirmedStateFactory(NisUtils.createTransactionValidatorFactory(timeProvider),
					cache -> (notification, context) -> {
					}, timeProvider, () -> new BlockHeight(511000), NisTestConstants.MAX_TRANSACTIONS_PER_BLOCK, new ForkConfiguration());
			this.transactions = creator.apply(factory, this.nisCache);
		}

		public UnconfirmedTransactions getTransactions() {
			return this.transactions;
		}

		public UnconfirmedTransactionsFilter getFilter() {
			return this.transactions.asFilter();
		}

		public ValidationResult add(final Transaction transaction) {
			return this.transactions.addNew(transaction);
		}

		public void addAll(final Collection<Transaction> transactions) {
			transactions.forEach(t -> this.transactions.addNew(prepare(t)));
		}

		@Override
		public Account addAccount(final Amount amount) {
			final Account account = Utils.generateRandomAccount();
			this.modifyCache(accountStateCache -> accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo()
					.incrementBalance(amount));
			this.transactions.removeAll(Collections.emptyList());
			return account;
		}

		public void setBalance(final Account account, final Amount amount) {
			this.modifyCache(accountStateCache -> accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo()
					.incrementBalance(amount));
		}

		protected void modifyCache(final Consumer<AccountStateCache> modify) {
			final NisCache nisCacheCopy = this.nisCache.copy();
			modify.accept(nisCacheCopy.getAccountStateCache());
			nisCacheCopy.commit();
		}
	}

	// endregion

	// region create transfers

	public static MockTransaction createMockTransaction(final UnconfirmedTransactionsTestContext context, final int customField) {
		final Account account = context.addAccount(Amount.fromNem(1_000));
		return prepare(new MockTransaction(account, customField, new TimeInstant(CURRENT_TIME + customField)));
	}

	public static List<Transaction> createMockTransactions(final UnconfirmedTransactionsTestContext context, final int startCustomField,
			final int endCustomField) {
		final List<Transaction> transactions = new ArrayList<>();

		for (int i = startCustomField; i <= endCustomField; ++i) {
			transactions.add(createMockTransaction(context, i));
		}

		return transactions;
	}

	public static List<Transaction> createMockTransactionsWithRandomTimeStamp(final Account account, final int count) {
		final List<Transaction> transactions = new ArrayList<>();
		final SecureRandom random = new SecureRandom();

		for (int i = 0; i < count; ++i) {
			final TimeInstant timeStamp = new TimeInstant(
					CURRENT_TIME + random.nextInt(BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME));
			transactions.add(prepare(new MockTransaction(account, i, timeStamp)));
		}

		return transactions;
	}

	public static Transaction createTransfer(final Account sender, final Account recipient, final int amount, final int fee) {
		final Transaction t = new TransferTransaction(1, new TimeInstant(CURRENT_TIME), sender, recipient, Amount.fromNem(amount), null);
		t.setFee(Amount.fromNem(fee));
		return prepare(t);
	}

	public static Transaction createTransferWithMosaicTransfer(final Account sender, final Account recipient, final int amount,
			final int fee, final MosaicId mosaicId, final int quantity) {
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaicId, new Quantity(quantity));
		final Transaction t = new TransferTransaction(2, new TimeInstant(CURRENT_TIME), sender, recipient, Amount.fromNem(amount),
				attachment);
		t.setFee(Amount.fromNem(fee));
		return prepare(t);
	}

	public static Transaction createImportanceTransfer(final Account sender, final Account remote, final int fee) {
		final Transaction t = new ImportanceTransferTransaction(new TimeInstant(CURRENT_TIME), sender, ImportanceTransferMode.Activate,
				remote);
		t.setFee(Amount.fromNem(fee));
		return prepare(t);
	}

	public static <T extends Transaction> T prepare(final T transaction) {
		prepareWithoutSignature(transaction);
		transaction.sign();
		return transaction;
	}

	public static <T extends Transaction> T prepareWithoutSignature(final T transaction) {
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(10));
		return transaction;
	}

	// endregion
}
