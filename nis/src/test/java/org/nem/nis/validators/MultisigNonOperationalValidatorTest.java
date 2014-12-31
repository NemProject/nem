package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.MockTransaction;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;

import java.util.ArrayList;
import java.util.Arrays;

public class MultisigNonOperationalValidatorTest {

	@Test
	public void nonMultisigAccountCanValidateAnyTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final Transaction transaction = new MockTransaction(account);
		context.addState(account);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void canValidateChildTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Transaction transaction = new MockTransaction(multisig);
		final Account cosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig, BlockHeight.ONE);

		// note, we're not signing transaction which means it's a child transaction

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigAccountCannotMakeMostTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Transaction transaction = new MockTransaction(multisig);
		final Account cosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig, BlockHeight.ONE);

		transaction.sign();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	@Test
	public void multisigAccountCanIssueMultisigModification() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final Account newCosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig, BlockHeight.ONE);

		final Transaction transaction = new MultisigSignerModificationTransaction(
				TimeInstant.ZERO,
				multisig,
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, newCosignatory)));
		transaction.sign();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigAccountCannotIssueMultisigDelModification() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final Account newCosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig, BlockHeight.ONE);

		// TODO: change this to del
		final Transaction transaction = new MultisigSignerModificationTransaction(
				TimeInstant.ZERO,
				multisig,
				Arrays.asList(new MultisigModification(MultisigModificationType.Unknown, newCosignatory)));
		transaction.sign();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	@Test
	public void multisigAccountCanIssueMultisigSignatureIfAlsoIsCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account deepMultisig = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		context.addState(deepMultisig);
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig, BlockHeight.ONE);
		context.makeCosignatory(multisig, deepMultisig, BlockHeight.ONE);

		final Transaction transaction = new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				multisig,
				Utils.generateRandomHash());
		transaction.sign();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	class TestContext {
		private final AccountStateCache accountCache = Mockito.mock(AccountStateCache.class);
		private final MultisigNonOperationalValidator validator = new MultisigNonOperationalValidator(accountCache);

		public ValidationResult validate(final Transaction transaction) {
			return validator.validate(transaction, new ValidationContext((final Account account, final Amount amount) -> true));
		}

		private AccountState addState(final Account account) {
			final Address address = account.getAddress();
			final AccountState state = new AccountState(address);
			Mockito.when(this.accountCache.findStateByAddress(address)).thenReturn(state);
			return state;
		}

		public void makeCosignatory(final Account signer, final Account multisig, final BlockHeight blockHeight) {
			this.accountCache.findStateByAddress(signer.getAddress()).getMultisigLinks().addMultisig(multisig.getAddress(), blockHeight);
			this.accountCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress(), blockHeight);
		}
	}
}
