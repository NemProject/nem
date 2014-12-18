package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;

import java.util.*;

public class MultisigSignatureValidatorTest {
	private static final BlockHeight BAD_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK - 1);
	private static final BlockHeight TEST_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK);

	@Test
	public void validatorCanValidateOtherTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(BlockHeight.ONE, context::debitPredicate));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigSignatureWithSignerNotBeingCosignatoryIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(Hash.ZERO);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT, context::debitPredicate));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG));
	}

	@Test
	public void multisigSignatureWithSignerBeingCosignatoryIsInvalidIfMultisigTransactionIsNotPresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(Hash.ZERO);
		final Account randomAccount = Utils.generateRandomAccount();
		context.addPoiState(randomAccount);
		context.makeCosignatory(context.signer, randomAccount);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT, context::debitPredicate));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG));
	}

	@Test
	public void multisigSignatureWithSignerBeingCosignatoryIsValid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account randomAccount = Utils.generateRandomAccount();
		final Account multisigInitiator = Utils.generateRandomAccount();
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(randomAccount, multisigInitiator);

		final Transaction transaction = context.createTransaction(HashUtils.calculateHash(multisigTransaction.getOtherTransaction()));

		context.addPoiState(randomAccount);
		context.makeCosignatory(context.signer, randomAccount);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT, context::debitPredicate));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigSignatureWithSignerBeingCosignatoryBelowForkIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(Hash.ZERO);
		final Account randomAccount = Utils.generateRandomAccount();
		context.addPoiState(randomAccount);
		context.makeCosignatory(context.signer, randomAccount);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(BAD_HEIGHT, context::debitPredicate));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
	}

	private class TestContext {
		private final AccountStateCache stateCache = Mockito.mock(AccountStateCache.class);
		private final MultisigSignatureValidator validator;
		private final Account signer = Utils.generateRandomAccount();
		private final List<Transaction> transactionList = new ArrayList<>();

		private TestContext() {
			this.validator = new MultisigSignatureValidator(this.stateCache, false, () -> this.transactionList);
		}

		private Transaction createTransaction(final Hash otherTransactionHash) {
			//signer.incrementBalance(Amount.fromNem(2001));
			this.addPoiState(this.signer);

			return new MultisigSignatureTransaction(
					TimeInstant.ZERO,
					this.signer,
					otherTransactionHash
			);
		}

		private MultisigTransaction createMultisigTransaction(final Account multisig, final Account cosignatory) {
			final TransferTransaction otherTransaction = new TransferTransaction(TimeInstant.ZERO,
					multisig,
					Utils.generateRandomAccount(),
					Amount.fromNem(123),
					null);
			final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, cosignatory, otherTransaction);
			transaction.sign();

			this.transactionList.add(transaction);

			return transaction;
		}

		private void addPoiState(final Account account) {
			final Address address = account.getAddress();
			final AccountState state = new AccountState(address);
			Mockito.when(this.stateCache.findStateByAddress(address))
					.thenReturn(state);
		}

		public void makeCosignatory(final Account signer, final Account multisig) {
			this.stateCache.findStateByAddress(signer.getAddress()).getMultisigLinks().addMultisig(multisig.getAddress(), TEST_HEIGHT);
			this.stateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress(), TEST_HEIGHT);
		}

		public boolean debitPredicate(final Account account, final Amount amount) {
			final Amount balance = this.stateCache.findStateByAddress(account.getAddress()).getAccountInfo().getBalance();
			return balance.compareTo(amount) >= 0;
		}
	}
}
