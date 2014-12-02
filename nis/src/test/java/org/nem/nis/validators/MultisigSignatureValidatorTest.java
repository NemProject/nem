package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;

public class MultisigSignatureValidatorTest {
	private static final BlockHeight BAD_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK - 1);
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
	public void multisigSignatureWithSignerNotBeingCosignatoryIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction();

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	@Test
	public void multisigSignatureWithSignerBeingCosignatoryOfAnyAccountIsValid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction();
		final Account randomAccount = Utils.generateRandomAccount();
		context.addPoiState(randomAccount);
		context.makeCosignatory(context.signer, randomAccount);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigSignatureWithSignerBeingCosignatoryBelowForkIsInalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction();
		final Account randomAccount = Utils.generateRandomAccount();
		context.addPoiState(randomAccount);
		context.makeCosignatory(context.signer, randomAccount);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(BAD_HEIGHT));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
	}

	private class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final MultisigSignatureValidator validator = new MultisigSignatureValidator(this.poiFacade);
		private final Account signer = Utils.generateRandomAccount();
		private final Signature signature = new Signature(Utils.generateRandomBytes(64));

		public Transaction createTransaction() {
			//signer.incrementBalance(Amount.fromNem(2001));
			this.addPoiState(signer);

			return new MultisigSignatureTransaction(
					TimeInstant.ZERO,
					signer,
					Hash.ZERO,
					signature
			);
		}

		private void addPoiState(final Account account) {
			final Address address = account.getAddress();
			final PoiAccountState state = new PoiAccountState(address);
			Mockito.when(this.poiFacade.findStateByAddress(address))
					.thenReturn(state);
		}

		public void makeCosignatory(final Account signer, final Account multisig) {
			this.poiFacade.findStateByAddress(signer.getAddress()).getMultisigLinks().addMultisig(multisig.getAddress(), TEST_HEIGHT);
			this.poiFacade.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress(), TEST_HEIGHT);
		}
	}
}
