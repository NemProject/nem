package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.MultisigTestContext;

import java.util.function.BiConsumer;

public class MultisigSignaturesPresentValidatorTest {
	private final BlockHeight FORK_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK);
	private final BlockHeight BAD_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK - 1);

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
	public void properTransactionWithSingleCosignerBelowForkDoesNotValidate() {
		assertProperTransaction(this.BAD_HEIGHT, ValidationResult.FAILURE_ENTITY_UNUSABLE);
	}

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
	public void properTransactionWithMultipleCosignersBelowForkDoesNotValidate() {
		this.assertProperTransactionMultiple(this.BAD_HEIGHT, ValidationResult.FAILURE_ENTITY_UNUSABLE, (ctx, t) -> {});
	}

	@Test
	public void properTransactionWithMultipleCosignersDoesNotValidateIfSignaturesAreMissing() {
		this.assertProperTransactionMultiple(this.FORK_HEIGHT, ValidationResult.FAILURE_MULTISIG_MISSING_COSIGNERS, (ctx, t) -> {});
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
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MISSING_COSIGNERS));
	}
	//endregion
}