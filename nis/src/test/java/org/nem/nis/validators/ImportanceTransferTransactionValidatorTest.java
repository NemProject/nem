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
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE));
	}

	//endregion

	//region recipient balance
	@Test
	public void activateImportanceTransferIsInvalidWhenRecipientHasBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = context.createTransaction(ImportanceTransferTransaction.Mode.Activate);
		((ImportanceTransferTransaction)transaction).getRemote().incrementBalance(Amount.fromNem(1));

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(BlockHeight.ONE));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_DESTINATION_ACCOUNT_HAS_NONZERO_BALANCE));
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
		context.setLessorRemoteState(transaction, BlockHeight.ONE, initialLink);
		context.setLesseeRemoteState(transaction, BlockHeight.ONE, initialLink);

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
		context.setLessorRemoteState(transaction, BlockHeight.ONE, initialLink);
		context.setLesseeRemoteState(transaction, BlockHeight.ONE, initialLink);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(new BlockHeight(1440)));

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
		context.setLessorRemoteState(transaction, BlockHeight.ONE, mode);
		context.setLesseeRemoteState(transaction, BlockHeight.ONE, mode);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(new BlockHeight(2882)));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	//endregion

	//region remote is already occupied
	@Test
	 public void cannotActivateIfRemoteIsActiveWithinOneDay()
	{
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Activate,
				new BlockHeight(1440),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotActivateIfRemoteIsActiveAfterOneDay()
	{
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Activate,
				new BlockHeight(1441),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void cannotActivateIfRemoteIsDeactivatedWithinOneDay()
	{
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Deactivate,
				ImportanceTransferTransaction.Mode.Activate,
				new BlockHeight(1440),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void canActivateIfRemoteIsDeactivatedAfterOneDay()
	{
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Deactivate,
				ImportanceTransferTransaction.Mode.Activate,
				new BlockHeight(1441),
				ValidationResult.SUCCESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsActiveWithinOneDay()
	{
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Deactivate,
				new BlockHeight(1440),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsActiveAfterOneDay()
	{
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Activate,
				ImportanceTransferTransaction.Mode.Deactivate,
				new BlockHeight(1441),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void cannotDeactivateIfRemoteIsDeactivatedWithinOneDay()
	{
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Deactivate,
				ImportanceTransferTransaction.Mode.Deactivate,
				new BlockHeight(1440),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsDeactivatedAfterOneDay()
	{
		// note that this will actually fail in validateOwner not validateRemote
		assertRemoteIsOccupiedTest(
				ImportanceTransferTransaction.Mode.Deactivate,
				ImportanceTransferTransaction.Mode.Deactivate,
				new BlockHeight(1441),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE);
	}

	private static void assertRemoteIsOccupiedTest(
			final ImportanceTransferTransaction.Mode previous,
			final ImportanceTransferTransaction.Mode mode,
			final BlockHeight blockHeight,
			final ValidationResult expectedValidationResult)
	{
		// Arrange:
		final TestContext context = new TestContext();

		// - use a dummy transaction to set the state of the remote account
		final ImportanceTransferTransaction dummy = context.createTransaction(ImportanceTransferTransaction.Mode.Activate);
		context.setLesseeRemoteState(dummy, BlockHeight.ONE, previous);

		// - create another transaction around the dummy remote account set up previously
		final Transaction transaction = context.createTransactionWithRemote(dummy.getRemote(), mode);

		// Act:
		final ValidationResult result = context.validator.validate(transaction, new ValidationContext(blockHeight));

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