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
	private static final BlockHeight TREASURY_REISSUANCE_BLOCK_HEIGHT = new BlockHeight(999);
	private static final BlockHeight[] OTHER_BLOCK_HEIGHTS = new BlockHeight[]{
			new BlockHeight(2), new BlockHeight(TREASURY_REISSUANCE_BLOCK_HEIGHT.getRaw() - 1),
			new BlockHeight(TREASURY_REISSUANCE_BLOCK_HEIGHT.getRaw() + 1), new BlockHeight(TREASURY_REISSUANCE_BLOCK_HEIGHT.getRaw() + 100)
	};

	@Test
	public void nemesisTransactionInNemesisBlockIsValid() {
		assertValidationResult(NEMESIS_ADDRESS, BlockHeight.ONE, ValidationResult.SUCCESS);
	}

	@Test
	public void nemesisTransactionInTreasuryReissuanceBlockIsValid() {
		assertValidationResult(NEMESIS_ADDRESS, TREASURY_REISSUANCE_BLOCK_HEIGHT, ValidationResult.SUCCESS);
	}

	@Test
	public void nemesisTransactionInOtherBlockIsInvalid() {
		for (final BlockHeight height : OTHER_BLOCK_HEIGHTS) {
			assertValidationResult(NEMESIS_ADDRESS, height, ValidationResult.FAILURE_NEMESIS_ACCOUNT_TRANSACTION_AFTER_NEMESIS_BLOCK);
		}
	}

	@Test
	public void otherTransactionInNemesisBlockIsValid() {
		assertValidationResult(Utils.generateRandomAddressWithPublicKey(), BlockHeight.ONE, ValidationResult.SUCCESS);
	}

	@Test
	public void otherTransactionInTreasuryReissuanceBlockIsValid() {
		assertValidationResult(Utils.generateRandomAddressWithPublicKey(), TREASURY_REISSUANCE_BLOCK_HEIGHT, ValidationResult.SUCCESS);
	}

	@Test
	public void otherTransactionInOtherBlockIsValid() {
		for (final BlockHeight height : OTHER_BLOCK_HEIGHTS) {
			assertValidationResult(Utils.generateRandomAddressWithPublicKey(), height, ValidationResult.SUCCESS);
		}
	}

	private static void assertValidationResult(final Address signerAddress, final BlockHeight height,
			final ValidationResult expectedResult) {
		// Arrange:
		final SingleTransactionValidator validator = new NemesisSinkValidator(TREASURY_REISSUANCE_BLOCK_HEIGHT);
		final Transaction transaction = new MockTransaction(new Account(signerAddress));

		// Act:
		final ValidationResult result = validator.validate(transaction, new ValidationContext(height, ValidationStates.Throw));

		// Assert:
		MatcherAssert.assertThat(String.format("height: %s", height), result, IsEqual.equalTo(expectedResult));
	}
}
