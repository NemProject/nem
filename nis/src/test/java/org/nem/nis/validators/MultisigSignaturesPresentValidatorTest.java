package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;

import java.util.function.BiConsumer;

public class MultisigSignaturesPresentValidatorTest {
	private final BlockHeight FORK_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK);
	private final BlockHeight BAD_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK - 1);

	//region other transactions
	@Test
	public void validatorCanValidateOtherTransactions() {
		assertCanValidateOtherTransactions(BlockHeight.ONE);
		assertCanValidateOtherTransactions(FORK_HEIGHT);
	}

	private static void assertCanValidateOtherTransactions(final BlockHeight blockHeight) {
		// Arrange:
		final TestContext context = new TestContext(true);
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(blockHeight, context::debitPredicate));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}
	//endregion

	//region single cosigner
	@Test
	public void properTransactionWithSingleCosignerBelowForkDoesNotValidate() {
		assertProperTransaction(BAD_HEIGHT, ValidationResult.FAILURE_ENTITY_UNUSABLE);
	}

	@Test
	public void properTransactionWithSingleCosignerValidates() {
		assertProperTransaction(FORK_HEIGHT, ValidationResult.SUCCESS);
	}

	private static void assertProperTransaction(final BlockHeight blockHeight, ValidationResult validationResult) {
		// Arrange:
		final TestContext context = new TestContext(true);
		final Transaction transaction = context.createTransaction();
		context.makeCosignatory(context.signer, context.multisig, blockHeight);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(blockHeight, context::debitPredicate));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}
	//endregion

	//region multiple cosigner
	@Test
	public void properTransactionWithMultipleCosignersBelowForkDoesNotValidate() {
		assertProperTransactionMultiple(BAD_HEIGHT, ValidationResult.FAILURE_ENTITY_UNUSABLE, (ctx, t) -> {});
	}

	@Test
	public void properTransactionWithMultipleCosignersDoesNotValidateIfSignaturesAreMissing() {
		assertProperTransactionMultiple(FORK_HEIGHT, ValidationResult.FAILURE_MULTISIG_MISSING_COSIGNERS, (ctx, t) -> {});
	}

	@Test
	public void properTransactionWithMultipleCosignersValidates() {
		assertProperTransactionMultiple(FORK_HEIGHT, ValidationResult.SUCCESS, (ctx, t) -> ctx.addSignature(ctx.dummy, (MultisigTransaction)t));
	}

	private void assertProperTransactionMultiple(final BlockHeight blockHeight, final ValidationResult validationResult, final BiConsumer<TestContext, Transaction> addSignature) {
		// Arrange:
		final TestContext context = new TestContext(true);
		final MultisigTransaction transaction = context.createTransaction();
		context.makeCosignatory(context.signer, context.multisig, blockHeight);
		context.makeCosignatory(context.dummy, context.multisig, blockHeight);

		addSignature.accept(context, transaction);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(blockHeight, context::debitPredicate));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	@Test
	public void signaturesOfAllCosignatoriesAreRequired() {
		// Arrange:
		final TestContext context = new TestContext(true);
		final Transaction transaction = context.createTransaction();
		context.makeCosignatory(context.signer, context.multisig, FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, FORK_HEIGHT);
		context.addSignature(context.dummy, (MultisigTransaction)transaction);

		final Account thirdAccount = Utils.generateRandomAccount();
		context.addPoiState(thirdAccount);
		context.makeCosignatory(thirdAccount, context.multisig, FORK_HEIGHT);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(FORK_HEIGHT, context::debitPredicate));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MISSING_COSIGNERS));
	}
	//endregion

	private static class TestContext {
		private final AccountStateCache accountCache = Mockito.mock(AccountStateCache.class);
		private final MultisigSignaturesPresentValidator validator;
		private final Account signer = Utils.generateRandomAccount();
		private final Account multisig = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final Account dummy = Utils.generateRandomAccount();

		private TestContext(final boolean blockCreation) {
			this.validator = new MultisigSignaturesPresentValidator(this.accountCache, blockCreation);
			this.addPoiState(this.signer);
			this.addPoiState(this.multisig);
			this.addPoiState(this.dummy);
		}

		public MultisigTransaction createTransaction() {
			final TransferTransaction otherTransaction = new TransferTransaction(TimeInstant.ZERO, multisig, recipient, Amount.fromNem(123), null);
			final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, signer, otherTransaction);
			transaction.sign();
			return transaction;
		}

		private void addPoiState(final Account account) {
			final Address address = account.getAddress();
			final AccountState state = new AccountState(address);
			Mockito.when(this.accountCache.findStateByAddress(address))
					.thenReturn(state);
		}

		public void makeCosignatory(final Account signer, final Account multisig, final BlockHeight blockHeight) {
			this.accountCache.findStateByAddress(signer.getAddress()).getMultisigLinks().addMultisig(multisig.getAddress(), blockHeight);
			this.accountCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress(), blockHeight);
		}

		private void addSignature(final Account signatureSigner, final MultisigTransaction multisigTransaction) {
			multisigTransaction.addSignature(new MultisigSignatureTransaction(TimeInstant.ZERO,
					signatureSigner,
					HashUtils.calculateHash(multisigTransaction.getOtherTransaction())));
		}

		public boolean debitPredicate(Account account, Amount amount) {
			final Amount balance = this.accountCache.findStateByAddress(account.getAddress()).getAccountInfo().getBalance();
			return balance.compareTo(amount) >= 0;
		}
	}
}