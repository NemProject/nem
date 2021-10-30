package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.MultisigTestContext;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MultisigCosignatoryModificationValidatorTest {
	private static final long MULTISIG_M_OF_N_FORK = BlockMarkerConstants
			.MULTISIG_M_OF_N_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24);

	// region add (single)

	@Test
	public void addingNewCosignatoryIsValid() {
		// Assert:
		assertSingleAddModificationValidationResult(context -> context.dummy, ValidationResult.SUCCESS);
	}

	@Test
	public void addingExistingCosignatoryIsInvalid() {
		// Assert:
		assertSingleAddModificationValidationResult(context -> context.signer, ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER);
	}

	private static void assertSingleAddModificationValidationResult(final Function<MultisigTestContext, Account> getModificationAccount,
			final ValidationResult expectedResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigCosignatoryModification modification = new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory,
				getModificationAccount.apply(context));
		final MultisigAggregateModificationTransaction transaction = context
				.createTypedMultisigModificationTransaction(Collections.singletonList(modification));
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region remove (single)

	@Test
	public void removingNonExistingCosignatoryIsInvalid() {
		// Assert:
		assertSingleDelModificationValidationResult(context -> context.dummy, ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER);
	}

	@Test
	public void removingExistingMultisigAccountIsInvalid() {
		// Assert:
		assertSingleDelModificationValidationResult(context -> context.multisig, ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER);
	}

	@Test
	public void removingExistingCosignatoryIsValid() {
		// Assert:
		assertSingleDelModificationValidationResult(context -> context.signer, ValidationResult.SUCCESS);
	}

	private static void assertSingleDelModificationValidationResult(final Function<MultisigTestContext, Account> getModificationAccount,
			final ValidationResult expectedResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigCosignatoryModification modification = new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory,
				getModificationAccount.apply(context));
		final MultisigAggregateModificationTransaction transaction = context
				.createTypedMultisigModificationTransaction(Collections.singletonList(modification));
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region add (multiple)

	@Test
	public void addingMultipleNewCosignatoriesIsValid() {
		// Arrange:
		final List<Account> otherAccounts = Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount(),
				Utils.generateRandomAccount());
		final MultisigTestContext context = new MultisigTestContext();
		otherAccounts.forEach(context::addState);

		final List<MultisigCosignatoryModification> modifications = otherAccounts.stream()
				.map(account -> new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, account))
				.collect(Collectors.toList());
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void addingSameNewCosignatoriesMultipleTimesIsInvalid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final List<MultisigCosignatoryModification> modifications = Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.dummy),
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.dummy));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MODIFICATION_REDUNDANT_MODIFICATIONS));
	}

	// endregion

	// region remove (multiple)

	@Test
	public void removingMultipleExistingCosignatoriesIsNotAllowed() {
		// Arrange:
		final List<Account> otherAccounts = Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount(),
				Utils.generateRandomAccount());
		final MultisigTestContext context = new MultisigTestContext();
		otherAccounts.forEach(account -> {
			context.addState(account);
			context.makeCosignatory(account, context.multisig);
		});

		final List<MultisigCosignatoryModification> modifications = otherAccounts.stream()
				.map(account -> new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, account))
				.collect(Collectors.toList());
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES));
	}

	@Test
	public void removingSameExistingCosignatoriesMultipleTimesIsInvalid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final List<MultisigCosignatoryModification> modifications = Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.signer),
				new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.signer));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MODIFICATION_REDUNDANT_MODIFICATIONS));
	}

	// endregion

	// region add / remove (multiple)

	@Test
	public void singleInvalidModificationInvalidatesAllModifications() {
		// Arrange:
		final List<Account> nonCosignerAccounts = Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount());
		final MultisigTestContext context = new MultisigTestContext();
		nonCosignerAccounts.forEach(context::addState);

		final List<MultisigCosignatoryModification> modifications = Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, nonCosignerAccounts.get(0)), // valid
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.signer), // invalid
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, nonCosignerAccounts.get(1))); // valid

		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER));
	}

	@Test
	public void canAddRemoveDifferentAccount() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final List<MultisigCosignatoryModification> modifications = Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.dummy),
				new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.signer));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void cannotAddRemoveSameCosignerAccountModifications() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final List<MultisigCosignatoryModification> modifications = Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.signer),
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.signer));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER));
	}

	@Test
	public void cannotAddRemoveSameNonCosignerAccountModifications() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final List<MultisigCosignatoryModification> modifications = Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.dummy),
				new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, context.dummy));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	// endregion

	// region prevent multisig account being cosignatory

	@Test
	public void cannotAddMultisigAccountAsCosignatory() {
		// Arrange:
		// - mark dummy as a multisig account by giving it a cosigner
		final MultisigTestContext context = new MultisigTestContext();
		final Account dummyCosigner = Utils.generateRandomAccount();
		context.addState(dummyCosigner);
		context.makeCosignatory(dummyCosigner, context.dummy);

		// - attempt to add dummy as a cosigner to multisig
		final List<MultisigCosignatoryModification> modifications = Collections
				.singletonList(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.dummy));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER));
	}

	@Test
	public void cannotCreateNewMultisigAccountWithMultisigAccountBeingCosignatory() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();

		// - attempt to create multisig account where the multisig account is cosigner of its own account
		final List<MultisigCosignatoryModification> modifications = Collections
				.singletonList(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.multisig));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER));
	}

	@Test
	public void cannotConvertAccountWhichIsCosignatoryOfAnAccountIntoMultisigAccount() {
		// Arrange:
		// - make multisig a cosigner of signer (this is the reverse of normal)
		final MultisigTestContext context = new MultisigTestContext();
		context.makeCosignatory(context.multisig, context.signer);

		// - attempt to make multisig a multisig by adding dummy as a cosigner
		final List<MultisigCosignatoryModification> modifications = Collections
				.singletonList(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.dummy));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER));
	}

	// endregion

	// region multisig aggregate modification transaction V2 fork

	@Test
	public void versionOneIsAcceptedBeforeFork() {
		assertValidationResultAtHeight(1, 1, ValidationResult.SUCCESS);
		assertValidationResultAtHeight(1, MULTISIG_M_OF_N_FORK - 1, ValidationResult.SUCCESS);
	}

	@Test
	public void versionOneIsAcceptedAtFork() {
		assertValidationResultAtHeight(1, MULTISIG_M_OF_N_FORK, ValidationResult.SUCCESS);
	}

	@Test
	public void versionOneIsAcceptedAfterFork() {
		assertValidationResultAtHeight(1, MULTISIG_M_OF_N_FORK + 1, ValidationResult.SUCCESS);
		assertValidationResultAtHeight(1, MULTISIG_M_OF_N_FORK + 100, ValidationResult.SUCCESS);
	}

	@Test
	public void versionTwoIsRejectedBeforeFork() {
		assertValidationResultAtHeight(2, 1, ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK);
		assertValidationResultAtHeight(2, MULTISIG_M_OF_N_FORK - 1,
				ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK);
	}

	@Test
	public void versionTwoIsAcceptedAtFork() {
		assertValidationResultAtHeight(2, MULTISIG_M_OF_N_FORK, ValidationResult.SUCCESS);
	}

	@Test
	public void versionTwoIsAcceptedAfterFork() {
		assertValidationResultAtHeight(2, MULTISIG_M_OF_N_FORK + 1, ValidationResult.SUCCESS);
		assertValidationResultAtHeight(2, MULTISIG_M_OF_N_FORK + 100, ValidationResult.SUCCESS);
	}

	private static void assertValidationResultAtHeight(final int version, final long height, final ValidationResult expectedResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigCosignatoryModification modification = new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory,
				context.dummy);
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(version,
				Collections.singletonList(modification));
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigCosignatoryModification(new BlockHeight(height), transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion
}
