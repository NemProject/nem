package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;

import java.util.Collections;

public class VersionTransactionValidatorTest {

	//region MULTISIG_M_OF_N_FORK
	private long testnetMOfNFork()
	{
		return BlockMarkerConstants.MULTISIG_M_OF_N_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24);
	}
	
	@Test
	public void v1MultisigModificationTransactionBeforeForkIsNotAllowed() {
		// Assert:
		assertValidation(
				createModificationTransaction(1),
				testnetMOfNFork() - 1,
				ValidationResult.SUCCESS);
	}

	@Test
	public void v1MultisigModificationTransactionAtForkIsAllowed() {
		// Assert:
		assertValidation(
				createModificationTransaction(1),
				testnetMOfNFork(),
				ValidationResult.SUCCESS);
	}

	@Test
	public void v1MultisigModificationTransactionAfterForkIsAllowed() {
		// Assert:
		assertValidation(
				createModificationTransaction(1),
				testnetMOfNFork() + 1,
				ValidationResult.SUCCESS);
	}

	@Test
	public void v2MultisigModificationTransactionBeforeForkIsNotAllowed() {
		// Assert:
		assertValidation(
				createModificationTransaction(2),
				testnetMOfNFork() - 1,
				ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK);
	}

	@Test
	public void v2MultisigModificationTransactionAtForkIsAllowed() {
		// Assert:
		assertValidation(
				createModificationTransaction(2),
				testnetMOfNFork(),
				ValidationResult.SUCCESS);
	}

	@Test
	public void v2MultisigModificationTransactionAfterForkIsAllowed() {
		// Assert:
		assertValidation(
				createModificationTransaction(2),
				testnetMOfNFork() + 1,
				ValidationResult.SUCCESS);
	}

	//endregion

	@Test
	public void otherTransactionIsAllowed() {
		// Assert:
		assertValidation(
				RandomTransactionFactory.createTransfer(),
				1,
				ValidationResult.SUCCESS);
	}

	private static MultisigAggregateModificationTransaction createModificationTransaction(final int version) {
		final MultisigAggregateModificationTransaction transaction = new MultisigAggregateModificationTransaction(
				version,
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount())),
				null);
		transaction.sign();
		return transaction;
	}

	private static void assertValidation(final Transaction transaction, final long blockHeight, final ValidationResult expectedResult) {
		// Arrange:
		final SingleTransactionValidator validator = new VersionTransactionValidator();
		final ValidationContext validationContext = new ValidationContext(new BlockHeight(blockHeight), DebitPredicates.Throw);

		// Act:
		final ValidationResult result = validator.validate(transaction, validationContext);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}