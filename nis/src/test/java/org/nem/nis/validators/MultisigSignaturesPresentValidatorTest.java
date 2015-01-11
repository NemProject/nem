package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.MultisigTestContext;

import java.util.*;
import java.util.function.BiConsumer;

public class MultisigSignaturesPresentValidatorTest {
	private final BlockHeight FORK_HEIGHT = new BlockHeight(123);

	//region other transactions

	@Test
	public void validatorCanValidateOtherTransactions() {
		assertCanValidateOtherTransactions(BlockHeight.ONE);
		assertCanValidateOtherTransactions(this.FORK_HEIGHT);
	}

	private static void assertCanValidateOtherTransactions(final BlockHeight blockHeight) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, blockHeight);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region single cosigner

	@Test
	public void properTransactionWithSingleCosignerValidates() {
		assertProperTransaction(this.FORK_HEIGHT, ValidationResult.SUCCESS);
	}

	private static void assertProperTransaction(final BlockHeight blockHeight, final ValidationResult validationResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig, blockHeight);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, blockHeight);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	//endregion

	//region multiple cosigner

	@Test
	public void properTransactionWithMultipleCosignersDoesNotValidateIfSignaturesAreMissing() {
		this.assertProperTransactionMultiple(this.FORK_HEIGHT, ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS, (ctx, t) -> {});
	}

	@Test
	public void properTransactionWithMultipleCosignersValidates() {
		this.assertProperTransactionMultiple(this.FORK_HEIGHT, ValidationResult.SUCCESS, (ctx, t) -> ctx.addSignature(ctx.dummy, (MultisigTransaction)t));
	}

	private void assertProperTransactionMultiple(final BlockHeight blockHeight, final ValidationResult validationResult, final BiConsumer<MultisigTestContext, Transaction> addSignature) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig, blockHeight);
		context.makeCosignatory(context.dummy, context.multisig, blockHeight);
		addSignature.accept(context, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, blockHeight);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	@Test
	public void signaturesOfAllCosignatoriesAreRequired() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, this.FORK_HEIGHT);
		context.addSignature(context.dummy, (MultisigTransaction)transaction);

		final Account thirdAccount = Utils.generateRandomAccount();
		context.addState(thirdAccount);
		context.makeCosignatory(thirdAccount, context.multisig, this.FORK_HEIGHT);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS));
	}

	//endregion

	//region multiple cosigner edge cases

	@Test
	public void validationSucceedsIfCreatorExplicitlySignsTransaction() {
		// Arrange:
		// - create a multisig account with two cosigners (signer, dummy)
		// - signer signature is automatically added because it signed transaction
		// - signer and dummy should both explicitly sign the transaction
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, this.FORK_HEIGHT);
		context.addSignature(context.signer, transaction);
		context.addSignature(context.dummy, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validationFailsIfAnySignatureContainsMismatchedHash() {
		// Arrange:
		// - create a multisig account with two cosigners (signer, dummy)
		// - signer signature is automatically added because it signed transaction
		// - dummy should sign the wrong transaction (a random hash)
		final MultisigTestContext context = new MultisigTestContext();
		MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, this.FORK_HEIGHT);

		// ugly but the only way to simulate a mismatched signature (MultisigTransaction does not allow one to be added)
		final Set<MultisigSignatureTransaction> signatures = new HashSet<>();
		signatures.add(new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				context.dummy,
				Utils.generateRandomHash()));

		transaction = Mockito.spy(transaction);
		Mockito.when(transaction.getCosignerSignatures()).thenReturn(signatures);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MISMATCHED_SIGNATURE));
	}

	@Test
	public void validationFailsIfAnyMultisigTransactionIsSignedByNonCosigner() {
		// Arrange:
		// - create a multisig account with two cosigners (signer, dummy)
		// - only signer signs the transaction
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, this.FORK_HEIGHT);

		// - have a non-cosigner sign the multisig account
		final Account nonCosignerAccount = Utils.generateRandomAccount();
		context.addSignature(nonCosignerAccount, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS));
	}

	@Test
	public void validationFailsIfAnyMultisigTransactionIsSignedByNonCosignerInAdditionToAllCosigners() {
		// Arrange:
		// - create a multisig account with two cosigners (signer, dummy)
		// - both signer and dummy sign the transaction
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, this.FORK_HEIGHT);
		context.addSignature(context.dummy, transaction);

		// - have a non-cosigner sign the multisig account
		final Account nonCosignerAccount = Utils.generateRandomAccount();
		context.addSignature(nonCosignerAccount, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS));
	}

	//endregion

	//region cosigner removal validation

	@Test
	public void removalOfMultisigRequiresSignatureFromAllAccountsNotBeingRemoved() {
		this.assertRemovalOfMultisigRequiresSignatureFromAllAccountsNotBeingRemoved(ValidationResult.SUCCESS, true);
	}

	@Test
	public void removalOfMultisigDoesNotRequireSignatureFromAccountBeingRemoved() {
		// Arrange:
		// - create a multisig transaction signed by signer that attempts to remove dummy
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigModificationTransaction(
				Arrays.asList(new MultisigModification(MultisigModificationType.Del, context.dummy)));

		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, this.FORK_HEIGHT);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert: this is allowable
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void removalOfMultisigDoesNotModifyMultisigLinks() {
		// Arrange:
		// - create a multisig transaction signed by signer that attempts to remove dummy
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigModificationTransaction(
				Arrays.asList(new MultisigModification(MultisigModificationType.Del, context.dummy)));

		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, this.FORK_HEIGHT);

		// Act:
		context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert:
		Assert.assertThat(context.getCosignatories(context.multisig).size(), IsEqual.equalTo(2));
	}

	@Test
	public void removalOfMultisigCanIncludeSignatureFromAccountBeingRemoved() {
		// Arrange:
		// - create a multisig transaction signed by signer and dummy that attempts to remove dummy
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigModificationTransaction(
				Arrays.asList(new MultisigModification(MultisigModificationType.Del, context.dummy)));

		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, this.FORK_HEIGHT);
		context.addSignature(context.dummy, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert: this is allowable
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void removalOfMultisigFailsIfMissingSignatureFromAnyAccountNotBeingRemoved() {
		this.assertRemovalOfMultisigRequiresSignatureFromAllAccountsNotBeingRemoved(ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS, false);
	}

	public void assertRemovalOfMultisigRequiresSignatureFromAllAccountsNotBeingRemoved(
			final ValidationResult validationResult,
			final boolean addSignatureOfThirdCosigner) {
		// Arrange:
		// - create a multisig account with three accounts: signer, dummy, and thirdAccount
		// - create a transaction to remove dummy
		// - signer implicitly signed the transaction because it created it
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigModificationTransaction(
				Arrays.asList(new MultisigModification(MultisigModificationType.Del, context.dummy)));

		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, this.FORK_HEIGHT);

		final Account thirdAccount = Utils.generateRandomAccount();
		context.addState(thirdAccount);
		context.makeCosignatory(thirdAccount, context.multisig, this.FORK_HEIGHT);

		if (addSignatureOfThirdCosigner) {
			context.addSignature(thirdAccount, (MultisigTransaction)transaction);
		}

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	@Test
	public void lastCosignerMustSignOwnRemovalAsCosigner() {
		// Arrange:
		// - create a multisig account with a single account: signer
		// - create a transaction to remove signer
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigModificationTransaction(
				Arrays.asList(new MultisigModification(MultisigModificationType.Del, context.signer)));
		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region cosigner removal edge cases

	@Test
	public void transactionComprisedOfMultipleCosignerDeletesIsRejected() {
		// Arrange:
		// - create a multisig account with three accounts: signer, dummy, and thirdAccount
		// - create a transaction to remove dummy and third
		// - signer implicitly signed the transaction because it created it
		final MultisigTestContext context = new MultisigTestContext();
		context.makeCosignatory(context.signer, context.multisig, this.FORK_HEIGHT);
		context.makeCosignatory(context.dummy, context.multisig, this.FORK_HEIGHT);

		final Account thirdAccount = Utils.generateRandomAccount();
		context.addState(thirdAccount);
		context.makeCosignatory(thirdAccount, context.multisig, this.FORK_HEIGHT);

		final MultisigTransaction transaction = context.createMultisigModificationTransaction(Arrays.asList(
				new MultisigModification(MultisigModificationType.Del, context.dummy),
				new MultisigModification(MultisigModificationType.Del, thirdAccount)));

		context.addSignature(context.dummy, transaction);
		context.addSignature(thirdAccount, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction, this.FORK_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	//endregion
}