package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.test.MultisigTestContext;

import java.util.*;
import java.util.function.BiConsumer;

public class MultisigSignaturesPresentValidatorTest {

	//region single cosigner

	@Test
	public void properTransactionWithSingleCosignerValidates() {
		assertProperTransaction(ValidationResult.SUCCESS);
	}

	private static void assertProperTransaction(final ValidationResult validationResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	//endregion

	//region multiple cosigner

	@Test
	public void properTransactionWithMultipleCosignersDoesNotValidateIfSignaturesAreMissing() {
		this.assertProperTransactionMultiple(ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS, (ctx, t) -> {});
	}

	@Test
	public void properTransactionWithMultipleCosignersValidates() {
		this.assertProperTransactionMultiple(ValidationResult.SUCCESS, (ctx, t) -> ctx.addSignature(ctx.dummy, (MultisigTransaction)t));
	}

	private void assertProperTransactionMultiple(final ValidationResult validationResult, final BiConsumer<MultisigTestContext, Transaction> addSignature) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig);
		context.makeCosignatory(context.dummy, context.multisig);
		addSignature.accept(context, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	@Test
	public void validateFailsIfLessThanMinCosignatoriesSignaturesArePresent() {
		assertValidationFailure(3, 5, 1, ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS);
		assertValidationFailure(3, 5, 2, ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS);
	}

	@Test
	public void validateSucceedsIfAtLeastMinCosignatoriesSignaturesArePresent() {
		assertValidationFailure(3, 5, 3, ValidationResult.SUCCESS);
		assertValidationFailure(3, 5, 4, ValidationResult.SUCCESS);
		assertValidationFailure(3, 5, 5, ValidationResult.SUCCESS);
	}

	private static void assertValidationFailure(
			final int minCosignatories,
			final int numCosignatories,
			final int numSignatures,
			final ValidationResult expectedResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		context.modifyMultisigAccount(minCosignatories, numCosignatories);
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.addSignatures(transaction, numSignatures - 1);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	//region multiple cosigner edge cases

	@Test
	public void validationSucceedsIfCreatorExplicitlySignsTransaction() {
		// Arrange:
		// - create a multisig account with two cosigners (signer, dummy)
		// - signer signature is automatically added because it signed transaction
		// - signer and dummy should both explicitly sign the transaction
		// - (note that this is more of an integration test since the MultisigTransaction is doing the filtering)
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig);
		context.makeCosignatory(context.dummy, context.multisig);
		context.addSignature(context.signer, transaction);
		context.addSignature(context.dummy, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));

		// Sanity:
		Assert.assertThat(transaction.getCosignerSignatures().size(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getSigners(), IsEquivalent.equivalentTo(context.dummy));
	}

	@Test
	public void validationSucceedsIfCosignerSignsTransactionMultipleTimes() {
		// Arrange:
		// - create a multisig account with two cosigners (signer, dummy)
		// - signer signature is automatically added because it signed transaction
		// - dummy should sign the transaction twice
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig);
		context.makeCosignatory(context.dummy, context.multisig);
		context.addSignature(context.dummy, transaction);
		context.addSignature(context.dummy, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));

		// Sanity:
		Assert.assertThat(transaction.getCosignerSignatures().size(), IsEqual.equalTo(1));
	}

	@Test
	public void validationFailsIfAnyMultisigTransactionIsSignedByNonCosigner() {
		// Arrange:
		// - create a multisig account with two cosigners (signer, dummy)
		// - only signer signs the transaction
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig);
		context.makeCosignatory(context.dummy, context.multisig);

		// - have a non-cosigner sign the multisig account
		final Account nonCosignerAccount = Utils.generateRandomAccount();
		context.addSignature(nonCosignerAccount, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

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
		context.makeCosignatory(context.signer, context.multisig);
		context.makeCosignatory(context.dummy, context.multisig);
		context.addSignature(context.dummy, transaction);

		// - have a non-cosigner sign the multisig account
		final Account nonCosignerAccount = Utils.generateRandomAccount();
		context.addSignature(nonCosignerAccount, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

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
		final MultisigTransaction transaction = context.createMultisigModificationTransaction(
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.dummy)));

		context.makeCosignatory(context.signer, context.multisig);
		context.makeCosignatory(context.dummy, context.multisig);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

		// Assert: this is allowable
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void removalOfMultisigDoesNotModifyMultisigLinks() {
		// Arrange:
		// - create a multisig transaction signed by signer that attempts to remove dummy
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigModificationTransaction(
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.dummy)));

		context.makeCosignatory(context.signer, context.multisig);
		context.makeCosignatory(context.dummy, context.multisig);

		// Act:
		context.validateSignaturePresent(transaction);

		// Assert:
		Assert.assertThat(context.getCosignatories(context.multisig).size(), IsEqual.equalTo(2));
	}

	@Test
	public void removalOfMultisigCanIncludeSignatureFromAccountBeingRemoved() {
		// Arrange:
		// - create a multisig transaction signed by signer and dummy that attempts to remove dummy
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigModificationTransaction(
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.dummy)));

		context.makeCosignatory(context.signer, context.multisig);
		context.makeCosignatory(context.dummy, context.multisig);
		context.addSignature(context.dummy, transaction);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

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
		final MultisigTransaction transaction = context.createMultisigModificationTransaction(
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.dummy)));

		context.makeCosignatory(context.signer, context.multisig);
		context.makeCosignatory(context.dummy, context.multisig);

		final Account thirdAccount = Utils.generateRandomAccount();
		context.addState(thirdAccount);
		context.makeCosignatory(thirdAccount, context.multisig);

		if (addSignatureOfThirdCosigner) {
			context.addSignature(thirdAccount, transaction);
		}

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	@Test
	public void lastCosignerMustSignOwnRemovalAsCosigner() {
		// Arrange:
		// - create a multisig account with a single account: signer
		// - create a transaction to remove signer
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigModificationTransaction(
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.signer)));
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region cosigner removal edge cases

	@Test
	public void transactionComprisedOfMultipleCosignerDeletesIsAllowed() {
		// Arrange:
		// (another validator blocks multiple deletes)
		// - create a multisig account with three accounts: signer, dummy, and thirdAccount
		// - create a transaction to remove dummy and third
		// - signer implicitly signed the transaction because it created it
		final MultisigTestContext context = new MultisigTestContext();
		context.makeCosignatory(context.signer, context.multisig);
		context.makeCosignatory(context.dummy, context.multisig);

		final Account thirdAccount = Utils.generateRandomAccount();
		context.addState(thirdAccount);
		context.makeCosignatory(thirdAccount, context.multisig);

		final MultisigTransaction transaction = context.createMultisigModificationTransaction(Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.dummy),
				new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, thirdAccount)));

		// Act:
		final ValidationResult result = context.validateSignaturePresent(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion
}