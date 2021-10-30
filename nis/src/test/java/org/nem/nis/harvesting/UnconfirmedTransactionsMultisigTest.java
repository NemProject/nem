package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.test.UnconfirmedTransactionsTestUtils;

import java.util.Collections;
import java.util.function.BiFunction;

import static org.nem.nis.test.UnconfirmedTransactionsTestUtils.prepare;
import static org.nem.nis.test.UnconfirmedTransactionsTestUtils.prepareWithoutSignature;

public abstract class UnconfirmedTransactionsMultisigTest implements UnconfirmedTransactionsTestUtils.UnconfirmedTransactionsTest {
	private static final TimeInstant CURRENT_TIME = new TimeInstant(UnconfirmedTransactionsTestUtils.CURRENT_TIME);
	private static final TimeInstant EXPIRY_TIME = CURRENT_TIME.addSeconds(-BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME - 1);

	// region multisig signature

	@Test
	public void properSignatureIsAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = this.createTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(CURRENT_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransaction();
		final ValidationResult signatureResult = context.addSignatureTransaction(signature);

		// Assert:
		MatcherAssert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(signatureResult, IsEqual.equalTo(ValidationResult.SUCCESS));
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
		MatcherAssert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(signatureResult, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}

	@Test
	public void multisigTransactionWithSignatureIsAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = this.createTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(CURRENT_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransactionWithSignature(signature);

		// Assert:
		MatcherAssert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigTransactionWithExpiredSignatureIsNotAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = this.createTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(EXPIRY_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransactionWithSignature(signature);

		// Assert:
		MatcherAssert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}

	// endregion

	// region dropExpiredTransactions

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
		MatcherAssert.assertThat(context.add(multisigTransaction), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		context.getTransactions().dropExpiredTransactions(CURRENT_TIME.addMinutes(5));

		// Assert:
		MatcherAssert.assertThat(context.getTransactions().size(), IsEqual.equalTo(0));
	}

	// endregion

	// region MultisigSignatureTestContext

	private MultisigSignatureTestContext createTestContext() {
		return new MultisigSignatureTestContext(this::createUnconfirmedTransactions);
	}

	private static class MultisigSignatureTestContext
			extends
				UnconfirmedTransactionsTestUtils.NonExecutingUnconfirmedTransactionsTestContext {
		private final Transaction t1;
		private final MultisigTransaction multisigTransaction;

		private final Account multisig = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final Account cosigner1 = Utils.generateRandomAccount();
		private final Account cosigner2 = Utils.generateRandomAccount();

		public MultisigSignatureTestContext(final BiFunction<UnconfirmedStateFactory, ReadOnlyNisCache, UnconfirmedTransactions> creator) {
			super(creator);
			this.makeCosignatory(this.cosigner1, this.multisig);
			this.makeCosignatory(this.cosigner2, this.multisig);

			this.setBalance(this.multisig, Amount.fromNem(121));
			this.setBalance(this.cosigner1, Amount.ZERO);
			this.setBalance(this.cosigner2, Amount.ZERO);

			// rebuild the state
			this.getTransactions().removeAll(Collections.emptyList());

			this.t1 = this.createTransferTransaction(CURRENT_TIME, Amount.fromNem(7));
			this.multisigTransaction = this.createMultisigTransaction(CURRENT_TIME, this.t1);
		}

		public MultisigSignatureTransaction createSignatureTransaction(final TimeInstant signatureTime) {
			return prepare(new MultisigSignatureTransaction(signatureTime, this.cosigner2, this.multisig, this.t1));
		}

		public MultisigTransaction createMultisigTransaction(final TimeInstant currentTime, final Transaction t1) {
			return prepare(new MultisigTransaction(currentTime, this.cosigner1, t1));
		}

		public TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Amount amount) {
			return prepareWithoutSignature(new TransferTransaction(timeStamp, this.multisig, this.recipient, amount, null));
		}

		public ValidationResult addMultisigTransaction() {
			return this.add(this.multisigTransaction);
		}

		public ValidationResult addMultisigTransactionWithSignature(final MultisigSignatureTransaction signatureTransaction) {
			this.multisigTransaction.addSignature(signatureTransaction);
			return this.add(this.multisigTransaction);
		}

		public ValidationResult addSignatureTransaction(final MultisigSignatureTransaction signatureTransaction) {
			return this.add(signatureTransaction);
		}

		public void makeCosignatory(final Account signer, final Account multisig) {
			this.modifyCache(accountStateCache -> {
				accountStateCache.findStateByAddress(signer.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
				accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress());
			});
		}
	}

	// endregion
}
