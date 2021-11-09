package org.nem.nis.validators.block;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.BlockValidator;

import java.util.Collections;

public class BlockMultisigAggregateModificationValidatorTest {
	private static final BlockValidator VALIDATOR = new BlockMultisigAggregateModificationValidator();

	// region passes validation

	@Test
	public void blockWithNoTransactionsValidates() {
		// Arrange:
		final Block block = NisUtils.createRandomBlock();

		// Assert:
		assertBlockValidation(block, ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithMultipleNonModificationTransactionsWithSameSignerValidates() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(new MockTransaction(signer));
		block.addTransaction(new MockTransaction(signer));

		// Assert:
		assertBlockValidation(block, ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithSingleModificationAndMultipleNonModificationTransactionsWithSameSignerValidates() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(new MockTransaction(signer));
		block.addTransaction(new MockTransaction(signer));
		block.addTransaction(createModificationTransaction(signer));

		// Assert:
		assertBlockValidation(block, ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithMultipleModificationsWithDifferentSignerValidates() {
		// Arrange:
		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(createModificationTransaction(Utils.generateRandomAccount()));
		block.addTransaction(createModificationTransactionInMultisig(Utils.generateRandomAccount()));
		block.addTransaction(createModificationTransaction(Utils.generateRandomAccount()));
		block.addTransaction(createModificationTransactionInMultisig(Utils.generateRandomAccount()));

		// Assert:
		assertBlockValidation(block, ValidationResult.SUCCESS);
	}

	// endregion

	// region fails validation

	@Test
	public void blockWithMultipleModificationsWithSameSignerDoesNotValidate() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(createModificationTransaction(signer));
		block.addTransaction(createModificationTransaction(signer));

		// Assert:
		assertBlockValidation(block, ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION);
	}

	@Test
	public void blockWithMultipleModificationsInMultisigTransactionsWithSameSignerDoesNotValidate() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(createModificationTransactionInMultisig(signer));
		block.addTransaction(createModificationTransactionInMultisig(signer));

		// Assert:
		assertBlockValidation(block, ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION);
	}

	@Test
	public void blockWithMultipleModificationsInAndOutOfMultisigTransactionsWithSameSignerDoesNotValidate() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(createModificationTransaction(signer));
		block.addTransaction(createModificationTransactionInMultisig(signer));

		// Assert:
		assertBlockValidation(block, ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION);
	}

	@Test
	public void blockWithSingleModificationConflictDoesNotValidate() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(createModificationTransaction(Utils.generateRandomAccount()));
		block.addTransaction(createModificationTransaction(signer));
		block.addTransaction(createModificationTransaction(Utils.generateRandomAccount()));
		block.addTransaction(createModificationTransaction(signer));

		// Assert:
		assertBlockValidation(block, ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION);
	}

	@Test
	public void blockWithMultipleModificationConflictsDoesNotValidate() {
		// Arrange:
		final Account signer1 = Utils.generateRandomAccount();
		final Account signer2 = Utils.generateRandomAccount();
		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(createModificationTransaction(signer1));
		block.addTransaction(createModificationTransaction(signer2));
		block.addTransaction(createModificationTransaction(signer1));
		block.addTransaction(createModificationTransaction(signer2));

		// Assert:
		assertBlockValidation(block, ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION);
	}

	// endregion

	private static Transaction createModificationTransactionInMultisig(final Account signer) {
		return new MultisigTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), createModificationTransaction(signer));
	}

	private static Transaction createModificationTransaction(final Account signer) {
		return new MultisigAggregateModificationTransaction(TimeInstant.ZERO, signer, Collections.singletonList(
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount())));
	}

	private static void assertBlockValidation(final Block block, final ValidationResult expectedResult) {
		// Act:
		final ValidationResult result = VALIDATOR.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}
