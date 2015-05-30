package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.nis.test.MultisigTestContext;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MultisigAggregateModificationTransactionValidatorTest {

	//region add (single)

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

	private static void assertSingleAddModificationValidationResult(
			final Function<MultisigTestContext, Account> getModificationAccount,
			final ValidationResult expectedResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigCosignatoryModification modification = new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, getModificationAccount.apply(context));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(Arrays.asList(modification));
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	//region remove (single)

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

	private static void assertSingleDelModificationValidationResult(
			final Function<MultisigTestContext, Account> getModificationAccount,
			final ValidationResult expectedResult) {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigCosignatoryModification modification = new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, getModificationAccount.apply(context));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(Arrays.asList(modification));
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	//region add / remove (multiple)

	//region add

	@Test
	public void addingMultipleNewCosignatoriesIsValid() {
		// Arrange:
		final List<Account> otherAccounts = Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Utils.generateRandomAccount());
		final MultisigTestContext context = new MultisigTestContext();
		otherAccounts.forEach(context::addState);

		final List<MultisigCosignatoryModification> modifications = otherAccounts.stream()
				.map(account -> new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, account))
				.collect(Collectors.toList());
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
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
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MODIFICATION_REDUNDANT_MODIFICATIONS));
	}

	//endregion

	//region remove

	@Test
	public void removingMultipleExistingCosignatoriesIsNotAllowed() {
		// Arrange:
		final List<Account> otherAccounts = Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Utils.generateRandomAccount());
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
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES));
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
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MODIFICATION_REDUNDANT_MODIFICATIONS));
	}

	//endregion

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
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER));
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
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
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
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER));
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
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	//endregion

	//region prevent multisig account being cosignatory

	@Test
	public void cannotAddMultisigAccountAsCosignatory() {
		// Arrange:
		// - mark dummy as a multisig account by giving it a cosigner
		final MultisigTestContext context = new MultisigTestContext();
		final Account dummyCosigner = Utils.generateRandomAccount();
		context.addState(dummyCosigner);
		context.makeCosignatory(dummyCosigner, context.dummy);

		// - attempt to add dummy as a cosigner to multisig
		final List<MultisigCosignatoryModification> modifications = Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.dummy));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER));
	}

	@Test
	public void cannotConvertAccountWhichIsCosignatoryOfAnAccountIntoMultisigAccount() {
		// Arrange:
		// - make multisig a cosigner of signer (this is the reverse of normal)
		final MultisigTestContext context = new MultisigTestContext();
		context.makeCosignatory(context.multisig, context.signer);

		// - attempt to make multisig a multisig by adding dummy as a cosigner
		final List<MultisigCosignatoryModification> modifications = Arrays.asList(
				new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, context.dummy));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER));
	}

	//endregion

	//region minCosignatories
	// The following tests alter a 2 of 4 multsig account

	@Test
	public void cannotChangeMinCosignatoriesToAValueLargerThanTheNumberOfCosignatories() {
		// 2 + 3 = 5 > 4 --> failure
		assertMinCosignatoriesModificationResult(3, ValidationResult.FAILURE_MULTISIG_MIN_COSIGNATORIES_OUT_OF_RANGE);
	}

	@Test
	public void canChangeMinCosignatoriesToAValueEqualToTheNumberOfCosignatories() {
		// 2 + 2 = 4 --> success
		assertMinCosignatoriesModificationResult(2, ValidationResult.SUCCESS);
	}

	@Test
	public void canChangeMinCosignatoriesToAValueLowerThanTheNumberOfCosignatories() {
		// 0 < i + 2 < 4 for i=1, 0, -1 --> success
		assertMinCosignatoriesModificationResult(1, ValidationResult.SUCCESS);
		assertMinCosignatoriesModificationResult(0, ValidationResult.SUCCESS);
		assertMinCosignatoriesModificationResult(-1, ValidationResult.SUCCESS);
	}

	// TODO 20150528 J-B: not sure why you want to throw if min < 0 but not if min == 0?
	// > i made the change to throw in the latter case too, but if you don't like it you can revert it
	// > otherwise, we can drop this test
	// TODO 20150530 BR -> J: min == 0 is not ok if there is at least one cosignatory
	@Test
	public void cannotChangeMinCosignatoriesToZeroIfTheNumberOfCosignatoriesIsLargerThanZero() {
		// -2 + 2 = 0 --> failure
		assertMinCosignatoriesModificationResult(-2, ValidationResult.FAILURE_MULTISIG_MIN_COSIGNATORIES_OUT_OF_RANGE);
	}

	private static void assertMinCosignatoriesModificationResult(
			final int relativeChange,
			final ValidationResult expectedResult) {
		// Arrange (make a 2 of 4 multisig account):
		final MultisigTestContext context = new MultisigTestContext();
		final List<Account> accounts = Arrays.asList(
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount());
		accounts.forEach(context::addState);
		context.makeCosignatory(accounts.get(0), context.multisig);
		context.makeCosignatory(accounts.get(1), context.multisig);
		context.makeCosignatory(accounts.get(2), context.multisig);
		context.makeCosignatory(context.signer, context.multisig);
		context.getMultisigLinks(context.multisig).incrementMinCosignatoriesBy(-2);
		final List<MultisigCosignatoryModification> modifications = new ArrayList<>();
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(
				modifications,
				new MultisigMinCosignatoriesModification(relativeChange));

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion
}
