package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.nis.test.MultisigTestContext;

import java.util.function.Function;

public class MultisigTransactionSignerValidatorTest {

	@Test
	public void multisigTransactionDoesNotValidateIfSignerIsNotCosignatory() {
		// Assert:
		assertValidationResult(context -> context.dummy, ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER);
	}

	@Test
	public void multisigTransactionDoesNotValidateIfSignerIsMultisigAccountCosignatory() {
		// Assert:
		assertValidationResult(context -> context.multisig, ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER);
	}

	@Test
	public void multisigTransactionValidatesIfSignerIsCosignatory() {
		// Assert:
		assertValidationResult(context -> context.signer, ValidationResult.SUCCESS);
	}

	private static void assertValidationResult(
			final Function<MultisigTestContext, Account> getMultisigSigner,
			final ValidationResult expectedResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigTransferTransaction(getMultisigSigner.apply(context));
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateTransaction(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//region other transactions

	@Test
	public void validatorCanValidateOtherTransactions() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final ValidationResult result = context.validateTransaction(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion
}
