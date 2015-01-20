package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.test.MultisigTestContext;

public class MultisigSignatureValidatorTest {
	private static final BlockHeight TEST_HEIGHT = new BlockHeight(123);

	@Test
	public void validatorCanValidateOtherTransactions() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final ValidationResult result = context.validateMultisigSignature(transaction, BlockHeight.ONE);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	// TODO 20150116 BR -> G: this tests succeeds, but rather because there is no corresponding multisig transaction like in the next test.
	@Test
	public void multisigSignatureWithSignerNotBeingCosignatoryIsInvalid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigSignature(Hash.ZERO, context.signer);

		// Act:
		final ValidationResult result = context.validateMultisigSignature(transaction, TEST_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG));
	}

	@Test
	public void multisigSignatureWithSignerBeingCosignatoryIsInvalidIfMultisigTransactionIsNotPresent() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigSignature(Hash.ZERO, context.signer);

		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigSignature(transaction, TEST_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG));
	}

	@Test
	public void multisigSignatureWithSignerBeingCosignatoryIsValid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction multisigTransaction = context.createMultisigTransferTransaction();
		final MultisigSignatureTransaction transaction = context.createMultisigSignature(HashUtils.calculateHash(multisigTransaction.getOtherTransaction()), context.dummy);

		context.makeCosignatory(context.dummy, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigSignature(transaction, TEST_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(multisigTransaction.getCosignerSignatures().size(), IsEqual.equalTo(1));
		Assert.assertThat(multisigTransaction.getCosignerSignatures().contains(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void multisigSignatureWithSignerBeingCosignatoryIsValidIfTransactionListContainsMoreThanOneMultisigTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		context.addRandomMultisigTransferTransactions(3);
		final MultisigTransaction multisigTransaction = context.createMultisigTransferTransaction();
		final MultisigSignatureTransaction transaction = context.createMultisigSignature(HashUtils.calculateHash(multisigTransaction.getOtherTransaction()), context.dummy);

		context.makeCosignatory(context.dummy, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigSignature(transaction, TEST_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(multisigTransaction.getCosignerSignatures().size(), IsEqual.equalTo(1));
		Assert.assertThat(multisigTransaction.getCosignerSignatures().contains(transaction), IsEqual.equalTo(true));
	}
}
