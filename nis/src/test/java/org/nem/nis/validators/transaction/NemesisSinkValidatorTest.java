package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

public class NemesisSinkValidatorTest {
	private static final Address NEMESIS_ADDRESS = NetworkInfos.getDefault().getNemesisBlockInfo().getAddress();

	@Test
	public void nemesisTransactionInNemesisBlockIsValid() {
		// Assert:
		assertValidationResult(NEMESIS_ADDRESS, BlockHeight.ONE, ValidationResult.SUCCESS);
	}

	@Test
	public void nemesisTransactionAfterNemesisBlockIsInvalid() {
		// Assert:
		assertValidationResult(NEMESIS_ADDRESS, new BlockHeight(2),
				ValidationResult.FAILURE_NEMESIS_ACCOUNT_TRANSACTION_AFTER_NEMESIS_BLOCK);
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

	private static void assertValidationResult(final Address signerAddress, final BlockHeight height,
			final ValidationResult expectedResult) {
		// Arrange:
		final SingleTransactionValidator validator = new NemesisSinkValidator();
		final Transaction transaction = new MockTransaction(new Account(signerAddress));

		// Act:
		final ValidationResult result = validator.validate(transaction, new ValidationContext(height, ValidationStates.Throw));

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}
