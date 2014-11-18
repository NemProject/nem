package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.poi.*;

public class ImportanceTransferTransactionValidatorTest {
	private static BlockHeight TEST_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_IT_VALIDATION_FORK);

	//region signer balance

	@Test
	public void activateImportanceTransferIsInvalidWithoutMinimumHarvestingBalanceAfterFee() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(ImportanceTransferTransaction.Mode.Activate);
		transaction.getSigner().decrementBalance(Amount.fromNem(1));

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
	}

	//endregion

	//region first link

	@Test
	public void activateImportanceTransferIsValidAsFirstLink() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(ImportanceTransferTransaction.Mode.Activate);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void deactivateImportanceTransferIsNotValidAsFirstLink() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(ImportanceTransferTransaction.Mode.Deactivate);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(TEST_HEIGHT));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE));
	}

	//endregion

	//region recipient balance
	@Test
	public void activateImportanceTransferIsInvalidWhenRecipientHasBalance() {
		assertActivateImportanceTransferIsInvalidWhenRecipientHasBalance(
				TEST_HEIGHT.getRaw(),
				ValidationResult.FAILURE_DESTINATION_ACCOUNT_HAS_NONZERO_BALANCE);
	}

	@Test
	public void activateImportanceTransferIsValidWhenRecipientHasBalanceBeforeForkBlock() {
		assertActivateImportanceTransferIsInvalidWhenRecipientHasBalance(TEST_HEIGHT.getRaw() - 1, ValidationResult.SUCCESS);
	}

	private static void assertActivateImportanceTransferIsInvalidWhenRecipientHasBalance(final long height, final ValidationResult validationResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(ImportanceTransferTransaction.Mode.Activate);
		((ImportanceTransferTransaction)transaction).getRemote().incrementBalance(Amount.fromNem(1));

		// Act:
		final BlockHeight testedHeight = new BlockHeight(height);
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(testedHeight));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
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
		final ImportanceTransferTransaction transaction = context.createTransaction(newLink);
		context.setLessorRemoteState(transaction, TEST_HEIGHT, initialLink);
		context.setLesseeRemoteState(transaction, TEST_HEIGHT, initialLink);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(1440 + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(testedHeight));

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
				ImportanceTransferTransaction.Mode.Activate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void deactivateImportanceTransferIsNotValidLessThanOneDayAfterActivateLink() {
		// Assert:
		assertTransferIsNotValidLessThanOneDayAfterOppositeLink(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Deactivate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	private static void assertTransferIsNotValidLessThanOneDayAfterOppositeLink(
			final ImportanceTransferTransaction.Mode initialLink,
			final ImportanceTransferTransaction.Mode newLink,
			final ValidationResult expectedValidationResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(newLink);
		context.setLessorRemoteState(transaction, TEST_HEIGHT, initialLink);
		context.setLesseeRemoteState(transaction, TEST_HEIGHT, initialLink);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(1439 + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(testedHeight));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	//endregion

	//region after same link

	@Test
	public void activateImportanceTransferIsNotValidAfterActivateLink() {
		// Assert:
		assertTransferIsNotValidAfterSameLink(
				ImportanceTransferTransaction.Mode.Activate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void deactivateImportanceTransferIsNotValidAfterDeactivateLink() {
		// Assert:
		assertTransferIsNotValidAfterSameLink(
				ImportanceTransferTransaction.Mode.Deactivate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE);
	}

	private static void assertTransferIsNotValidAfterSameLink(
			final ImportanceTransferTransaction.Mode mode,
			final ValidationResult expectedValidationResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(mode);
		context.setLessorRemoteState(transaction, TEST_HEIGHT, mode);
		context.setLesseeRemoteState(transaction, TEST_HEIGHT, mode);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(2882 + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(testedHeight));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	//endregion

	//region remote is already occupied
	@Test
	public void cannotActivateIfRemoteIsActiveWithinOneDay() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Activate,
				new BlockHeight(1439),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotActivateIfRemoteIsActiveAfterOneDay() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Activate,
				new BlockHeight(1440),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void cannotActivateIfRemoteIsDeactivatedWithinOneDay() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Deactivate,
				ImportanceTransferTransaction.Mode.Activate,
				new BlockHeight(1439),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void canActivateIfRemoteIsDeactivatedAfterOneDay() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Deactivate,
				ImportanceTransferTransaction.Mode.Activate,
				new BlockHeight(1440),
				ValidationResult.SUCCESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsActiveWithinOneDay() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Deactivate,
				new BlockHeight(1439),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsActiveAfterOneDay() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Deactivate,
				new BlockHeight(1440),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void cannotDeactivateIfRemoteIsDeactivatedWithinOneDay() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Deactivate,
				ImportanceTransferTransaction.Mode.Deactivate,
				new BlockHeight(1439),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsDeactivatedAfterOneDay() {
		// note that this will actually fail in validateOwner not validateRemote
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Deactivate,
				ImportanceTransferTransaction.Mode.Deactivate,
				new BlockHeight(1440),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE);
	}

	//endregion

	//region transitive remote harvesting

	@Test
	public void remoteHarvesterCannotActivateHisOwnRemoteHarvesterWithinOneDay() {
		assertRemoteHarvesterCannotActivateHisOwnRemoteHarvester(
				new BlockHeight(1439),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	// two following tests, are testing following scenario
	// 1) A importance transfer to X
	// 2) send some nems to X
	// 3) X importance transfer to Y
	//
	// I'm not sure if we handle such situation properly (obviously importance transfer should not be transitive)
	@Test
	public void remoteHarvesterCannotActivateHisOwnRemoteHarvesterAfterOneDay() {
		assertRemoteHarvesterCannotActivateHisOwnRemoteHarvester(
				new BlockHeight(1440),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	private static void assertRemoteHarvesterCannotActivateHisOwnRemoteHarvester(
			final BlockHeight height,
			final ValidationResult validationResult) {
		final TestContext context = new TestContext();

		// - use a dummy transaction to set the state of the remote account
		final ImportanceTransferTransaction dummy = context.createTransaction(ImportanceTransferTransaction.Mode.Activate);
		context.setLesseeRemoteState(dummy, TEST_HEIGHT, ImportanceTransferTransaction.Mode.Activate);

		// - create another transaction around the dummy remote account set up previously
		final Account furtherRemote = Utils.generateRandomAccount();
		final Account remote = dummy.getRemote();
		final Transaction transaction = new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				remote,
				ImportanceTransferTransaction.Mode.Activate,
				furtherRemote);
		context.addRemoteLinks(furtherRemote);
		remote.incrementBalance(Amount.fromNem(2001));

		// Act:
		final BlockHeight testedHeight = new BlockHeight(height.getRaw() + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(testedHeight));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	private static void assertRemoteIsOccupiedTest(
			final ImportanceTransferTransaction.Mode previous,
			final ImportanceTransferTransaction.Mode mode,
			final BlockHeight blockHeight,
			final ValidationResult expectedValidationResult) {
		// Arrange:
		final TestContext context = new TestContext();

		// - use a dummy transaction to set the state of the remote account
		final ImportanceTransferTransaction dummy = context.createTransaction(ImportanceTransferTransaction.Mode.Activate);
		context.setLesseeRemoteState(dummy, TEST_HEIGHT, previous);

		// - create another transaction around the dummy remote account set up previously
		final Transaction transaction = context.createTransactionWithRemote(dummy.getRemote(), mode);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(blockHeight.getRaw() + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(testedHeight));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
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

		private ImportanceTransferTransaction createTransaction(final ImportanceTransferTransaction.Mode mode) {
			final Account signer = Utils.generateRandomAccount();
			final Account remote = Utils.generateRandomAccount();
			signer.incrementBalance(Amount.fromNem(2001));
			this.addRemoteLinks(signer);
			this.addRemoteLinks(remote);
			return new ImportanceTransferTransaction(
					TimeInstant.ZERO,
					signer,
					mode,
					remote);
		}

		private void addRemoteLinks(final Account account) {
			final Address address = account.getAddress();
			final PoiAccountState state = new PoiAccountState(address);
			Mockito.when(this.poiFacade.findStateByAddress(address))
					.thenReturn(state);
		}

		private void setLessorRemoteState(final ImportanceTransferTransaction account, final BlockHeight height, final ImportanceTransferTransaction.Mode mode) {
			final Address sender = account.getSigner().getAddress();
			final Address remote = account.getRemote().getAddress();
			final RemoteLink link = new RemoteLink(remote, height, mode.value(), RemoteLink.Owner.HarvestingRemotely);
			this.poiFacade.findStateByAddress(sender).getRemoteLinks().addLink(link);
		}

		private void setLesseeRemoteState(final ImportanceTransferTransaction account, final BlockHeight height, final ImportanceTransferTransaction.Mode mode) {
			final Address sender = account.getSigner().getAddress();
			final Address remote = account.getRemote().getAddress();
			final RemoteLink link = new RemoteLink(sender, height, mode.value(), RemoteLink.Owner.RemoteHarvester);
			this.poiFacade.findStateByAddress(remote).getRemoteLinks().addLink(link);
		}

		public ImportanceTransferTransaction createTransactionWithRemote(final Account remote, final ImportanceTransferTransaction.Mode mode) {
			final Account signer = Utils.generateRandomAccount(Amount.fromNem(2001));
			this.addRemoteLinks(signer);
			return new ImportanceTransferTransaction(
					TimeInstant.ZERO,
					signer,
					mode,
					remote);
		}
	}
}