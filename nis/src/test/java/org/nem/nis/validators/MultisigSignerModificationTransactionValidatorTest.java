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
	public void addingNewCosignatoryIsValid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigSignerModificationTransaction(MultisigModificationType.Add);

		// Act:
		final ValidationResult result = context.validateSignerModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void addingExistingCosignatoryIsInvalid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		// TODO 20150103 J-G: so i understand, the context is returning a transaction that will make context.signer a cosignatory of context.multisig?
		final Transaction transaction = context.createMultisigSignerModificationTransaction(MultisigModificationType.Add);
		context.makeCosignatory(context.signer, context.multisig, BlockHeight.ONE);

		// Act:
		final ValidationResult result = context.validateSignerModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER));
	}

	@Test
	public void removingNonExistingCosignatoryIsInvalid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigSignerModificationTransaction(MultisigModificationType.Del);

		// Act:
		final ValidationResult result = context.validateSignerModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	@Test
	public void removingExistingCosignatoryIsValid() {
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
