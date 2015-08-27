package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.test.*;

import java.util.function.*;

public abstract class UnconfirmedTransactionsMultisigTest {
	final static TimeInstant CURRENT_TIME = new TimeInstant(10_000);
	final static TimeInstant EXPIRY_TIME = CURRENT_TIME.addSeconds(-BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME - 1);

	/**
	 * Creates the unconfirmed transactions cache.
	 *
	 * @param unconfirmedStateFactory The unconfirmed state factory to use.
	 * @param nisCache The NIS cache to use.
	 * @return The unconfirmed transactions cache.
	 */
	public abstract UnconfirmedTransactions createUnconfirmedTransactions(
			final UnconfirmedStateFactory unconfirmedStateFactory,
			final ReadOnlyNisCache nisCache);

	//region multisig signature

	@Test
	public void properSignatureIsAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = this.createTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(CURRENT_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransaction();
		final ValidationResult signatureResult = context.addSignatureTransaction(signature);

		// Assert:
		Assert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(signatureResult, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void expiredSignatureIsNotAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = this.createTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(EXPIRY_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransaction();
		final ValidationResult signatureResult = context.addSignatureTransaction(signature);

		// Assert:
		Assert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(signatureResult, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}

	@Test
	public void multisigTransactionWithSignatureIsAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = this.createTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(CURRENT_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransactionWithSignature(signature);

		// Assert:
		Assert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigTransactionWithExpiredSignatureIsNotAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = this.createTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(EXPIRY_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransactionWithSignature(signature);

		// Assert:
		Assert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}

	//endregion

	//region dropExpiredTransactions

	@Test
	public void dropExpiredTransactionsDropsMultisigTransactionWithExpiredSignature() {
		this.assertTransactionIsDropped(CURRENT_TIME.addHours(1), CURRENT_TIME.addMinutes(1));
	}

	@Test
	public void dropExpiredTransactionsDropsMultisigTransactionWithExpiredInnerTransaction() {
		this.assertTransactionIsDropped(CURRENT_TIME.addMinutes(1), CURRENT_TIME.addHours(1));
	}

	private void assertTransactionIsDropped(final TimeInstant innerTransactionDeadline, final TimeInstant signatureDeadline) {
		// Arrange:
		final MultisigSignatureTestContext context = this.createTestContext();
		context.t1.setDeadline(innerTransactionDeadline);
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(CURRENT_TIME, context.t1);
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(CURRENT_TIME);
		signature.setDeadline(signatureDeadline);
		signature.sign();
		multisigTransaction.addSignature(signature);
		Assert.assertThat(context.transactions.addExisting(multisigTransaction), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		context.transactions.dropExpiredTransactions(CURRENT_TIME.addMinutes(5));

		// Assert:
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	//endregion

	//region MultisigSignatureTestContext

	private MultisigSignatureTestContext createTestContext() {
		return new MultisigSignatureTestContext(this::createUnconfirmedTransactions);
	}

	private static class MultisigSignatureTestContext {
		private final Transaction t1;
		private final MultisigTransaction multisigTransaction;

		private final Account multisig = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final Account cosigner1 = Utils.generateRandomAccount();
		private final Account cosigner2 = Utils.generateRandomAccount();

		private final ReadOnlyNisCache nisCache = NisCacheFactory.createReal();
		private final UnconfirmedTransactions transactions;

		public MultisigSignatureTestContext(final BiFunction<UnconfirmedStateFactory, ReadOnlyNisCache, UnconfirmedTransactions> creator) {
			this.makeCosignatory(this.cosigner1, this.multisig);
			this.makeCosignatory(this.cosigner2, this.multisig);

			this.setBalance(this.multisig, Amount.fromNem(121));
			this.setBalance(this.cosigner1, Amount.ZERO);
			this.setBalance(this.cosigner2, Amount.ZERO);

			this.t1 = this.createTransferTransaction(CURRENT_TIME, Amount.fromNem(7));
			this.multisigTransaction = this.createMultisigTransaction(CURRENT_TIME, this.t1);

			final TimeProvider timeProvider = Utils.createMockTimeProvider(CURRENT_TIME.getRawTime());
			final UnconfirmedStateFactory factory = new UnconfirmedStateFactory(
					NisUtils.createTransactionValidatorFactory(timeProvider),
					cache -> (notification, context) -> { },
					timeProvider,
					BlockHeight.MAX::prev);
			this.transactions = creator.apply(factory, this.nisCache);
		}

		//region create transaction

		public MultisigSignatureTransaction createSignatureTransaction(final TimeInstant signatureTime) {
			return prepareAndSign(new MultisigSignatureTransaction(signatureTime, this.cosigner2, this.multisig, this.t1));
		}

		public MultisigTransaction createMultisigTransaction(final TimeInstant currentTime, final Transaction t1) {
			return prepareAndSign(new MultisigTransaction(currentTime, this.cosigner1, t1));
		}

		public TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Amount amount) {
			return prepare(new TransferTransaction(timeStamp, this.multisig, this.recipient, amount, null));
		}

		private static <T extends Transaction> T prepare(final T transaction) {
			transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));
			return transaction;
		}

		private static <T extends Transaction> T prepareAndSign(final T transaction) {
			prepare(transaction);
			transaction.sign();
			return transaction;
		}

		//endregion

		//region add transaction

		public ValidationResult addMultisigTransaction() {
			return this.transactions.addExisting(this.multisigTransaction);
		}

		public ValidationResult addMultisigTransactionWithSignature(final MultisigSignatureTransaction signatureTransaction) {
			this.multisigTransaction.addSignature(signatureTransaction);
			return this.transactions.addExisting(this.multisigTransaction);
		}

		public ValidationResult addSignatureTransaction(final MultisigSignatureTransaction signatureTransaction) {
			return this.transactions.addExisting(signatureTransaction);
		}

		//endregion

		//region modify state

		public void makeCosignatory(final Account signer, final Account multisig) {
			this.modifyCache(accountStateCache -> {
				accountStateCache.findStateByAddress(signer.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
				accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress());
			});
		}

		public void setBalance(final Account multisig, final Amount amount) {
			this.modifyCache(accountStateCache ->
					accountStateCache.findStateByAddress(multisig.getAddress()).getAccountInfo().incrementBalance(amount));
		}

		private void modifyCache(final Consumer<AccountStateCache> modify) {
			final NisCache nisCacheCopy = this.nisCache.copy();
			modify.accept(nisCacheCopy.getAccountStateCache());
			nisCacheCopy.commit();
		}

		//endregion
	}

	//endregion
}
