package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

import java.util.Collections;

public class VersionTransactionValidatorTest {
	private static final long MULTISIG_M_OF_N_FORK = BlockMarkerConstants.MULTISIG_M_OF_N_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24);
	private static final long MOSAICS_FORK = BlockMarkerConstants.MOSAICS_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24);

	//region MULTISIG_M_OF_N_FORK

	@Test
	public void v1MultisigModificationTransactionIsAlwaysAllowed() {
		// Assert:
		assertAlwaysAllowed(
				createModificationTransaction(1),
				MULTISIG_M_OF_N_FORK);
	}

	@Test
	public void v2MultisigModificationTransactionIsOnlyAllowedAtAndAfterFork() {
		// Assert:
		assertOnlyAllowedAtAndAfterFork(
				createModificationTransaction(2),
				MULTISIG_M_OF_N_FORK,
				ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK);
	}

	//endregion

	//region MOSAICS_FORK

	@Test
	public void provisionNamespaceTransactionIsOnlyAllowedAtAndAfterFork() {
		// Assert:
		assertOnlyAllowedAtAndAfterFork(
				RandomTransactionFactory.createProvisionNamespaceTransaction(),
				MOSAICS_FORK,
				ValidationResult.FAILURE_TRANSACTION_BEFORE_SECOND_FORK);
	}

	@Test
	public void mosaicDefinitionCreationTransactionIsOnlyAllowedAtAndAfterFork() {
		// Assert:
		assertOnlyAllowedAtAndAfterFork(
				RandomTransactionFactory.createMosaicDefinitionCreationTransaction(),
				MOSAICS_FORK,
				ValidationResult.FAILURE_TRANSACTION_BEFORE_SECOND_FORK);
	}

	@Test
	public void mosaicSupplyChangeTransactionIsOnlyAllowedAtAndAfterFork() {
		// Assert:
		assertOnlyAllowedAtAndAfterFork(
				RandomTransactionFactory.createMosaicSupplyChangeTransaction(),
				MOSAICS_FORK,
				ValidationResult.FAILURE_TRANSACTION_BEFORE_SECOND_FORK);
	}

	@Test
	public void v2TransferTransactionIsOnlyAllowedAtAndAfterFork() {
		// Assert:
		assertOnlyAllowedAtAndAfterFork(
				createTransferTransaction(2),
				MOSAICS_FORK,
				ValidationResult.FAILURE_TRANSACTION_BEFORE_SECOND_FORK);
	}

	@Test
	public void v1TransferTransactionIsAlwaysAllowed() {
		// Assert:
		assertAlwaysAllowed(
				createTransferTransaction(1),
				MOSAICS_FORK);
	}

	//endregion

	@Test
	public void otherTransactionIsAllowed() {
		// Assert:
		assertValidation(
				RandomTransactionFactory.createImportanceTransfer(),
				1,
				ValidationResult.SUCCESS);
	}

	private static TransferTransaction createTransferTransaction(final int version) {
		return new TransferTransaction(
				version,
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(111),
				new TransferTransactionAttachment());

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

	private static void assertOnlyAllowedAtAndAfterFork(final Transaction transaction, final long forkHeight, final  ValidationResult expectedFailure) {
		assertValidation(transaction, forkHeight - 1, expectedFailure);
		assertValidation(transaction, forkHeight, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight + 1, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight + 100, ValidationResult.SUCCESS);
	}

	private static void assertAlwaysAllowed(final Transaction transaction, final long forkHeight) {
		assertValidation(transaction, 1, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight - 1, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight + 1, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight + 100, ValidationResult.SUCCESS);
	}

	private static void assertValidation(final Transaction transaction, final long blockHeight, final ValidationResult expectedResult) {
		// Arrange:
		final SingleTransactionValidator validator = new VersionTransactionValidator();
		final ValidationContext validationContext = new ValidationContext(new BlockHeight(blockHeight), ValidationStates.Throw);

		// Act:
		final ValidationResult result = validator.validate(transaction, validationContext);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}