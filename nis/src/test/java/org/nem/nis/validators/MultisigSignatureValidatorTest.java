package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.MultisigTestContext;

public class MultisigSignatureValidatorTest {
	private static final BlockHeight BAD_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK - 1);
	private static final BlockHeight TEST_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK);

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

		context.makeCosignatory(context.signer, context.multisig, TEST_HEIGHT);

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
		final Transaction transaction = context.createMultisigSignature(HashUtils.calculateHash(multisigTransaction.getOtherTransaction()), context.dummy);

		context.makeCosignatory(context.dummy, context.multisig, TEST_HEIGHT);

		// Act:
		final ValidationResult result = context.validateMultisigSignature(transaction, TEST_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigSignatureWithSignerBeingCosignatoryBelowForkIsInvalid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigSignature(Hash.ZERO, context.signer);

		// Act:
		final ValidationResult result = context.validateMultisigSignature(transaction, BAD_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
	}

}
