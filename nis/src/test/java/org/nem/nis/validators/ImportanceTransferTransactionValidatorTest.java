package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.poi.*;

public class ImportanceTransferTransactionValidatorTest {

	//region signer balance

	@Test
	public void activateImportanceTransferIsInvalidWithoutMinimumHarvestingBalanceAfterFee() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(ImportanceTransferTransaction.Mode.Activate);
		transaction.getSigner().decrementBalance(Amount.fromNem(1));

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(BlockHeight.ONE));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
	}

	// TODO 20141005 J-G: (minor pedantic comment) - can you keep blanklines around //region //endregion :)
	//endregion

	//region first link

	@Test
	public void activateImportanceTransferIsValidAsFirstLink() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(ImportanceTransferTransaction.Mode.Activate);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(BlockHeight.ONE));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void deactivateImportanceTransferIsNotValidAsFirstLink() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(ImportanceTransferTransaction.Mode.Deactivate);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(BlockHeight.ONE));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
	}

	//endregion

	//region one day after opposite link

	@Test
	public void activateImportanceTransferIsValidOneDayAfterDeactivateLink() {
		// Assert:
		assertTransferIsValidOneDayAfterOppositeLink(
				ImportanceTransferTransaction.Mode.Deactivate,
				ImportanceTransferTransaction.Mode.Activate);
	}

	@Test
	public void deactivateImportanceTransferIsValidOneDayAfterActivateLink() {
		// Assert:
		assertTransferIsValidOneDayAfterOppositeLink(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Deactivate);
	}

	private static void assertTransferIsValidOneDayAfterOppositeLink(
			final ImportanceTransferTransaction.Mode initialLink,
			final ImportanceTransferTransaction.Mode newLink) {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(newLink);
		context.setRemoteState(transaction.getSigner(), BlockHeight.ONE, initialLink);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(new BlockHeight(1441)));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region less than one day after opposite link

	@Test
	public void activateImportanceTransferIsNotValidLessThanOneDayAfterDeactivateLink() {
		// Assert:
		assertTransferIsNotValidLessThanOneDayAfterOppositeLink(
				ImportanceTransferTransaction.Mode.Deactivate,
				ImportanceTransferTransaction.Mode.Activate);
	}

	@Test
	public void deactivateImportanceTransferIsNotValidLessThanOneDayAfterActivateLink() {
		// Assert:
		assertTransferIsNotValidLessThanOneDayAfterOppositeLink(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Deactivate);
	}

	private static void assertTransferIsNotValidLessThanOneDayAfterOppositeLink(
			final ImportanceTransferTransaction.Mode initialLink,
			final ImportanceTransferTransaction.Mode newLink) {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(newLink);
		context.setRemoteState(transaction.getSigner(), BlockHeight.ONE, initialLink);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(new BlockHeight(1440)));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
	}

	//endregion

	//region after same link

	@Test
	public void activateImportanceTransferIsNotValidAfterActivateLink() {
		// Assert:
		assertTransferIsNotValidAfterSameLink(ImportanceTransferTransaction.Mode.Activate);
	}

	@Test
	public void deactivateImportanceTransferIsNotValidAfterDeactivateLink() {
		// Assert:
		assertTransferIsNotValidAfterSameLink(ImportanceTransferTransaction.Mode.Deactivate);
	}

	private static void assertTransferIsNotValidAfterSameLink(final ImportanceTransferTransaction.Mode mode) {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(mode);
		context.setRemoteState(transaction.getSigner(), BlockHeight.ONE, mode);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(new BlockHeight(2882)));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
	}

	//endregion

	//region other type

	@Test
	public void otherTransactionTypesPassValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount(Amount.fromNem(100));
		final MockTransaction transaction = new MockTransaction(account);
		transaction.setFee(Amount.fromNem(200));

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(BlockHeight.ONE));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	private static class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final ImportanceTransferTransactionValidator validator = new ImportanceTransferTransactionValidator(
				this.poiFacade,
				Amount.fromNem(2000));

		private Transaction createTransaction(final ImportanceTransferTransaction.Mode mode) {
			final Account signer = Utils.generateRandomAccount();
			signer.incrementBalance(Amount.fromNem(2001));
			this.addRemoteLinks(signer);
			return new ImportanceTransferTransaction(
					TimeInstant.ZERO,
					signer,
					mode,
					Utils.generateRandomAccount());
		}

		private void addRemoteLinks(final Account account) {
			final Address address = account.getAddress();
			final PoiAccountState state = new PoiAccountState(address);
			Mockito.when(this.poiFacade.findStateByAddress(address))
					.thenReturn(state);
		}

		private void setRemoteState(final Account account, final BlockHeight height, final ImportanceTransferTransaction.Mode mode) {
			final RemoteLink link = new RemoteLink(account.getAddress(), height, mode.value(), RemoteLink.Owner.HarvestingRemotely);
			this.poiFacade.findStateByAddress(account.getAddress()).getRemoteLinks().addLink(link);
		}
	}
}