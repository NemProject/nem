package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;

public class MultisigTransactionValidatorTest {
	private static final BlockHeight TEST_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK);

	@Test
	public void validatorCanValidateOtherTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(BlockHeight.ONE));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigTransactionDoesNotValidateIfSignerIsNotCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction();

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	@Test
	public void multisigTransactionValidatesIfSignerIsCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction();
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigTransactionDoesNotValidateBelowForkBLock() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction();
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT.prev()));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
	}

	private static class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final MultisigTransactionValidator validator = new MultisigTransactionValidator(this.poiFacade);
		private final Account signer = Utils.generateRandomAccount();
		private final Account multisig = Utils.generateRandomAccount();

		private MultisigTransaction createTransaction() {
			final Account recipient = Utils.generateRandomAccount();
			signer.incrementBalance(Amount.fromNem(2001));
			this.addPoiState(signer);
			this.addPoiState(multisig);
			final Transaction otherTransaction = new TransferTransaction(
					TimeInstant.ZERO,
					multisig,
					recipient,
					Amount.fromNem(1234),
					null
			);
			return new MultisigTransaction(
					TimeInstant.ZERO,
					signer,
					otherTransaction);
		}

		private void addPoiState(final Account account) {
			final Address address = account.getAddress();
			final PoiAccountState state = new PoiAccountState(address);
			Mockito.when(this.poiFacade.findStateByAddress(address))
					.thenReturn(state);
		}

		public void makeCosignatory(final Account signer, final Account multisig) {
			this.poiFacade.findStateByAddress(signer.getAddress()).addMultisig(multisig.getAddress(), TEST_HEIGHT);
			this.poiFacade.findStateByAddress(multisig.getAddress()).addCosignatory(signer.getAddress(), TEST_HEIGHT);
		}
	}
}
