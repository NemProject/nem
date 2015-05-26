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
		final MultisigModification modification = new MultisigModification(MultisigModificationType.AddCosignatory, getModificationAccount.apply(context));
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
		final MultisigModification modification = new MultisigModification(MultisigModificationType.DelCosignatory, getModificationAccount.apply(context));
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

		final List<MultisigModification> modifications = otherAccounts.stream()
				.map(account -> new MultisigModification(MultisigModificationType.AddCosignatory, account))
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
		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.AddCosignatory, context.dummy),
				new MultisigModification(MultisigModificationType.AddCosignatory, context.dummy));
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

		final List<MultisigModification> modifications = otherAccounts.stream()
				.map(account -> new MultisigModification(MultisigModificationType.DelCosignatory, account))
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
		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.DelCosignatory, context.signer),
				new MultisigModification(MultisigModificationType.DelCosignatory, context.signer));
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

		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.AddCosignatory, nonCosignerAccounts.get(0)), // valid
				new MultisigModification(MultisigModificationType.AddCosignatory, context.signer), // invalid
				new MultisigModification(MultisigModificationType.AddCosignatory, nonCosignerAccounts.get(1))); // valid

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
		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.AddCosignatory, context.dummy),
				new MultisigModification(MultisigModificationType.DelCosignatory, context.signer));
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
		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.DelCosignatory, context.signer),
				new MultisigModification(MultisigModificationType.AddCosignatory, context.signer));
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
		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.AddCosignatory, context.dummy),
				new MultisigModification(MultisigModificationType.DelCosignatory, context.dummy));
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
		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.AddCosignatory, context.dummy));
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
		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.AddCosignatory, context.dummy));
		final MultisigAggregateModificationTransaction transaction = context.createTypedMultisigModificationTransaction(modifications);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER));
	}

	//endregion
}
