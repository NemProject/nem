package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
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
		final MultisigModification modification = new MultisigModification(MultisigModificationType.Add, getModificationAccount.apply(context));
		final Transaction transaction = context.createMultisigModificationTransaction(Arrays.asList(modification)).getOtherTransaction();
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
		final MultisigModification modification = new MultisigModification(MultisigModificationType.Del, getModificationAccount.apply(context));
		final Transaction transaction = context.createMultisigModificationTransaction(Arrays.asList(modification)).getOtherTransaction();
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
				.map(account -> new MultisigModification(MultisigModificationType.Add, account))
				.collect(Collectors.toList());
		final Transaction transaction = context.createMultisigModificationTransaction(modifications).getOtherTransaction();
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
				new MultisigModification(MultisigModificationType.Add, context.dummy),
				new MultisigModification(MultisigModificationType.Add, context.dummy));
		final Transaction transaction = context.createMultisigModificationTransaction(modifications).getOtherTransaction();
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MODIFICATION_REDUNDANT_MODIFICATIONS));
	}

	//endregion

	//region remove

	@Test
	public void removingMultipleExistingCosignatoriesIsInvalid() {
		// Arrange:
		final List<Account> otherAccounts = Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Utils.generateRandomAccount());
		final MultisigTestContext context = new MultisigTestContext();
		otherAccounts.forEach(account -> {
			context.addState(account);
			context.makeCosignatory(account, context.multisig);
		});

		final List<MultisigModification> modifications = otherAccounts.stream()
				.map(account -> new MultisigModification(MultisigModificationType.Del, account))
				.collect(Collectors.toList());
		final Transaction transaction = context.createMultisigModificationTransaction(modifications).getOtherTransaction();
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
				new MultisigModification(MultisigModificationType.Del, context.signer),
				new MultisigModification(MultisigModificationType.Del, context.signer));
		final Transaction transaction = context.createMultisigModificationTransaction(modifications).getOtherTransaction();
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES));
	}

	//endregion

	@Test
	public void singleInvalidModificationInvalidatesAllModifications() {
		// Arrange:
		final List<Account> nonCosignerAccounts = Arrays.asList(Utils.generateRandomAccount(), Utils.generateRandomAccount());
		final MultisigTestContext context = new MultisigTestContext();
		nonCosignerAccounts.forEach(context::addState);

		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.Add, nonCosignerAccounts.get(0)), // valid
				new MultisigModification(MultisigModificationType.Add, context.signer), // invalid
				new MultisigModification(MultisigModificationType.Add, nonCosignerAccounts.get(1))); // valid

		final Transaction transaction = context.createMultisigModificationTransaction(modifications).getOtherTransaction();
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
				new MultisigModification(MultisigModificationType.Add, context.dummy),
				new MultisigModification(MultisigModificationType.Del, context.signer));
		final Transaction transaction = context.createMultisigModificationTransaction(modifications).getOtherTransaction();
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
				new MultisigModification(MultisigModificationType.Del, context.signer),
				new MultisigModification(MultisigModificationType.Add, context.signer));
		final Transaction transaction = context.createMultisigModificationTransaction(modifications).getOtherTransaction();
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
				new MultisigModification(MultisigModificationType.Add, context.dummy),
				new MultisigModification(MultisigModificationType.Del, context.dummy));
		final Transaction transaction = context.createMultisigModificationTransaction(modifications).getOtherTransaction();
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	//endregion

	//region other transactions

	@Test
	public void canValidateOtherTransactions() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion
}
