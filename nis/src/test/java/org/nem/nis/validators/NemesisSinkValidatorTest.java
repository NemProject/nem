package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.test.DebitPredicates;

public class NemesisSinkValidatorTest {

	@Test
	public void nemesisTransactionInNemesisBlockIsValid() {
		// Assert:
		assertValidationResult(NemesisBlock.ADDRESS, BlockHeight.ONE, ValidationResult.SUCCESS);
	}

	@Test
	public void nemesisTransactionAfterNemesisBlockIsInvalid() {
		// Assert:
		assertValidationResult(NemesisBlock.ADDRESS, new BlockHeight(2), ValidationResult.FAILURE_NEMESIS_ACCOUNT_TRANSACTION_AFTER_NEMESIS_BLOCK);
	}

	@Test
	public void otherTransactionInNemesisBlockIsValid() {
		// Assert:
		assertValidationResult(Utils.generateRandomAddressWithPublicKey(), BlockHeight.ONE, ValidationResult.SUCCESS);
	}

	@Test
	public void otherTransactionAfterNemesisBlockIsValid() {
		// Assert:
		assertValidationResult(Utils.generateRandomAddressWithPublicKey(), new BlockHeight(2), ValidationResult.SUCCESS);
	}

	private static void assertValidationResult(
			final Address signerAddress,
			final BlockHeight height,
			final ValidationResult expectedResult) {
		// Arrange:
		final SingleTransactionValidator validator = new NemesisSinkValidator();
		final Transaction transaction = new MockTransaction(new Account(signerAddress));

		// Act:
		final ValidationResult result = validator.validate(transaction, new ValidationContext(height, DebitPredicates.Throw));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}