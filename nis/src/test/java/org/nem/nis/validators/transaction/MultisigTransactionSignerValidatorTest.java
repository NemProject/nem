package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
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

	private static void assertValidationResult(final Function<MultisigTestContext, Account> getMultisigSigner,
			final ValidationResult expectedResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction transaction = context.createMultisigTransferTransaction(getMultisigSigner.apply(context));
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateTransaction(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}
