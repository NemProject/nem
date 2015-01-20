package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.MultisigTestContext;

import java.util.Arrays;

public class MultisigNonOperationalValidatorTest {

	@Test
	public void nonMultisigAccountCanValidateAnyTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account account = Utils.generateRandomAccount();
		final Transaction transaction = new MockTransaction(account);
		context.addState(account);

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void canValidateChildTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Transaction transaction = new MockTransaction(multisig);
		final Account cosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig);

		// note, we're not signing transaction which means it's a child transaction
		// TODO 20150103 J-B don't like this

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigAccountCannotMakeMostTransactions() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Transaction transaction = new MockTransaction(multisig);
		final Account cosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig);

		transaction.sign();

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	// TODO 20150103 J-G: why do we want to allow this?
	@Test
	public void multisigAccountCanIssueMultisigModification() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final Account newCosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig);

		final Transaction transaction = new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				multisig,
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, newCosignatory)));
		transaction.sign();

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigAccountCannotIssueMultisigDelModification() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final Account newCosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig);

		final Transaction transaction = new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				multisig,
				// TODO 20150103 J-G: consider refactoring as the type and expected reslt are only differences
				Arrays.asList(new MultisigModification(MultisigModificationType.Del, newCosignatory)));
		transaction.sign();

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	// TODO 20150103 J-G: is there a good reason for supporting this?
	@Test
	public void multisigAccountCanIssueMultisigSignatureIfAlsoIsCosignatory() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account deepMultisig = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		context.addState(deepMultisig);
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig);
		context.makeCosignatory(multisig, deepMultisig);

		final Transaction transaction = new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				multisig,
				Utils.generateRandomHash());
		transaction.sign();

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}
}
