package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicConstants;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.nis.ForkConfiguration;

import java.util.*;

public class FeeSinkNonOperationalValidatorTest {

	private static void assertValidation(final Account multisigAccount, BlockHeight validationHeight,
			final ValidationResult expectedResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext(multisigAccount);
		final MultisigTransaction transaction = context.createMultisigTransferTransaction();

		final ForkConfiguration forkConfiguration = new ForkConfiguration(new BlockHeight(1234), new ArrayList<Hash>(),
				new ArrayList<Hash>());
		final FeeSinkNonOperationalValidator validator = new FeeSinkNonOperationalValidator(forkConfiguration);

		// Act:
		final ValidationResult result = validator.validate(transaction, new ValidationContext(validationHeight, ValidationStates.Throw));

		// Assert
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	@Test
	public void nonSinkAccountCanInitiateTransactionsInAllBlocks() {
		final Account account = Utils.generateRandomAccount();
		assertValidation(account, new BlockHeight(1), ValidationResult.SUCCESS);
		assertValidation(account, new BlockHeight(1233), ValidationResult.SUCCESS);
		assertValidation(account, new BlockHeight(1234), ValidationResult.SUCCESS);
		assertValidation(account, new BlockHeight(1245), ValidationResult.SUCCESS);
		assertValidation(account, new BlockHeight(2000), ValidationResult.SUCCESS);
	}

	@Test
	public void mosaicSinkAccountCanInitiateTransactionsOnlyInBlocksBeforeOrAtFork() {
		final Account account = MosaicConstants.MOSAIC_CREATION_FEE_SINK;
		assertValidation(account, new BlockHeight(1), ValidationResult.SUCCESS);
		assertValidation(account, new BlockHeight(1233), ValidationResult.SUCCESS);
		assertValidation(account, new BlockHeight(1234), ValidationResult.SUCCESS);
		assertValidation(account, new BlockHeight(1245), ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG);
		assertValidation(account, new BlockHeight(2000), ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG);
	}

	@Test
	public void namespaceSinkAccountCanInitiateTransactionsOnlyInBlocksBeforeOrAtFork() {
		final Account account = MosaicConstants.NAMESPACE_OWNER_NEM;
		assertValidation(account, new BlockHeight(1), ValidationResult.SUCCESS);
		assertValidation(account, new BlockHeight(1233), ValidationResult.SUCCESS);
		assertValidation(account, new BlockHeight(1234), ValidationResult.SUCCESS);
		assertValidation(account, new BlockHeight(1245), ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG);
		assertValidation(account, new BlockHeight(2000), ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG);
	}
}
