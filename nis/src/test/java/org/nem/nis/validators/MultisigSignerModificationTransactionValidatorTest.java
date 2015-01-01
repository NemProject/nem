package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.MultisigModificationType;
import org.nem.core.model.Transaction;
import org.nem.core.model.ValidationResult;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.test.MultisigTestContext;

public class MultisigSignerModificationTransactionValidatorTest {
	@Test
	public void canValidateOtherTransactions() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final ValidationResult result = context.validateSignerModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void addingNewCosingatoryIsValid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigSignerModificationTransaction(MultisigModificationType.Add);

		// Act:
		final ValidationResult result = context.validateSignerModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void addingExistingCosingatoryIsInvalid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigSignerModificationTransaction(MultisigModificationType.Add);
		context.makeCosignatory(context.signer, context.multisig, BlockHeight.ONE);

		// Act:
		final ValidationResult result = context.validateSignerModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER));
	}

	@Test
	public void removingNonExistingCosingatoryIsInvalid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigSignerModificationTransaction(MultisigModificationType.Del);

		// Act:
		final ValidationResult result = context.validateSignerModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	@Test
	public void removingExistingCosingatoryIsValid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigSignerModificationTransaction(MultisigModificationType.Del);
		context.makeCosignatory(context.signer, context.multisig, BlockHeight.ONE);

		// Act:
		final ValidationResult result = context.validateSignerModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}
}
