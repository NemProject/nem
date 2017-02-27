package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.ValidationContext;

public class ImportanceTransferTransactionValidatorTest {
	private static final BlockHeight TEST_HEIGHT = new BlockHeight(123);
	private static final int ABOVE_LIMIT = NisTestConstants.REMOTE_HARVESTING_DELAY;
	private static final int BELOW_LIMIT = ABOVE_LIMIT - 1;

	//region first link

	@Test
	public void activateImportanceTransferIsValidAsFirstLink() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(ImportanceTransferMode.Activate);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void deactivateImportanceTransferIsNotValidAsFirstLink() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(ImportanceTransferMode.Deactivate);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE));
	}

	//endregion

	//region account is acceptable as remote validation

	@Test
	public void activateImportanceTransferIsInvalidWhenRecipientHasBalance() {
		assertActivateImportanceTransferIsInvalidWhenRecipientHasBalance(
				TEST_HEIGHT.getRaw(),
				ValidationResult.FAILURE_DESTINATION_ACCOUNT_HAS_PREEXISTING_BALANCE_TRANSFER);
	}

	private static void assertActivateImportanceTransferIsInvalidWhenRecipientHasBalance(final long height, final ValidationResult validationResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(ImportanceTransferMode.Activate);
		context.getAccountInfo(transaction.getRemote()).incrementBalance(Amount.fromNem(1));

		// Act:
		final BlockHeight testedHeight = new BlockHeight(height);
		final ValidationResult result = context.validate(transaction, testedHeight);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	@Test
	public void activateImportanceTransferIsInvalidWhenRemoteOwnsMosaic() {
		// Arrange: make remote account own a mosaic
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(ImportanceTransferMode.Activate);
		context.getAccountInfo(transaction.getRemote()).addMosaicId(Utils.createMosaicId(123));

		// Act:
		final ValidationResult result = context.validate(transaction, TEST_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_DESTINATION_ACCOUNT_OWNS_MOSAIC));
	}

	@Test
	public void activateImportanceTransferIsInvalidWhenRemoteIsMultisig() {
		// Arrange: make remote account multisig
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(ImportanceTransferMode.Activate);
		context.getAccountState(transaction.getRemote()).getMultisigLinks().addCosignatory(Utils.generateRandomAddress());

		// Act:
		final ValidationResult result = context.validate(transaction, TEST_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_DESTINATION_ACCOUNT_IS_MULTISIG));
	}

	@Test
	public void activateImportanceTransferIsInvalidWhenRemoteIsCosignatory() {
		// Arrange: make remote account cosignatory
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(ImportanceTransferMode.Activate);
		context.getAccountState(transaction.getRemote()).getMultisigLinks().addCosignatoryOf(Utils.generateRandomAddress());

		// Act:
		final ValidationResult result = context.validate(transaction, TEST_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_DESTINATION_ACCOUNT_IS_COSIGNER));
	}

	//endregion

	//region one day after opposite link

	@Test
	public void activateImportanceTransferIsValidOneDayAfterDeactivateLink() {
		// Assert:
		assertTransferIsValidOneDayAfterOppositeLink(
				ImportanceTransferMode.Deactivate,
				ImportanceTransferMode.Activate);
	}

	@Test
	public void deactivateImportanceTransferIsValidOneDayAfterActivateLink() {
		// Assert:
		assertTransferIsValidOneDayAfterOppositeLink(
				ImportanceTransferMode.Activate,
				ImportanceTransferMode.Deactivate);
	}

	private static void assertTransferIsValidOneDayAfterOppositeLink(
			final ImportanceTransferMode initialLink,
			final ImportanceTransferMode newLink) {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(newLink);
		context.setLessorRemoteState(transaction, TEST_HEIGHT, initialLink);
		context.setLesseeRemoteState(transaction, TEST_HEIGHT, initialLink);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(ABOVE_LIMIT + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validate(transaction, testedHeight);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region less than one day after opposite link

	@Test
	public void activateImportanceTransferIsNotValidLessThanOneDayAfterDeactivateLink() {
		// Assert:
		assertTransferIsNotValidLessThanOneDayAfterOppositeLink(
				ImportanceTransferMode.Deactivate,
				ImportanceTransferMode.Activate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void deactivateImportanceTransferIsNotValidLessThanOneDayAfterActivateLink() {
		// Assert:
		assertTransferIsNotValidLessThanOneDayAfterOppositeLink(
				ImportanceTransferMode.Activate,
				ImportanceTransferMode.Deactivate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	private static void assertTransferIsNotValidLessThanOneDayAfterOppositeLink(
			final ImportanceTransferMode initialLink,
			final ImportanceTransferMode newLink,
			final ValidationResult expectedValidationResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(newLink);
		context.setLessorRemoteState(transaction, TEST_HEIGHT, initialLink);
		context.setLesseeRemoteState(transaction, TEST_HEIGHT, initialLink);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(BELOW_LIMIT + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validate(transaction, testedHeight);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	//endregion

	//region after same link

	@Test
	public void activateImportanceTransferIsNotValidAfterActivateLink() {
		// Assert:
		assertTransferIsNotValidAfterSameLink(
				ImportanceTransferMode.Activate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void deactivateImportanceTransferIsNotValidAfterDeactivateLink() {
		// Assert:
		assertTransferIsNotValidAfterSameLink(
				ImportanceTransferMode.Deactivate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE);
	}

	private static void assertTransferIsNotValidAfterSameLink(
			final ImportanceTransferMode mode,
			final ValidationResult expectedValidationResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(mode);
		context.setLessorRemoteState(transaction, TEST_HEIGHT, mode);
		context.setLesseeRemoteState(transaction, TEST_HEIGHT, mode);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(2882 + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validate(transaction, testedHeight);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	//endregion

	//region remote is already occupied
	@Test
	public void cannotActivateIfRemoteIsActiveBelowLimit() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferMode.Activate,
				ImportanceTransferMode.Activate,
				new BlockHeight(BELOW_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotActivateIfRemoteIsActiveAboveLimit() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferMode.Activate,
				ImportanceTransferMode.Activate,
				new BlockHeight(ABOVE_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void cannotActivateIfRemoteIsDeactivatedBelowLimit() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferMode.Deactivate,
				ImportanceTransferMode.Activate,
				new BlockHeight(BELOW_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void canActivateIfRemoteIsDeactivatedAboveLimit() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferMode.Deactivate,
				ImportanceTransferMode.Activate,
				new BlockHeight(ABOVE_LIMIT),
				ValidationResult.SUCCESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsActiveBelowLimit() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferMode.Activate,
				ImportanceTransferMode.Deactivate,
				new BlockHeight(BELOW_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsActiveAboveLimit() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferMode.Activate,
				ImportanceTransferMode.Deactivate,
				new BlockHeight(ABOVE_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void cannotDeactivateIfRemoteIsDeactivatedBelowLimit() {
		assertRemoteIsOccupiedTest(
				ImportanceTransferMode.Deactivate,
				ImportanceTransferMode.Deactivate,
				new BlockHeight(BELOW_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsDeactivatedAboveLimit() {
		// note that this will actually fail in validateOwner not validateRemote
		assertRemoteIsOccupiedTest(
				ImportanceTransferMode.Deactivate,
				ImportanceTransferMode.Deactivate,
				new BlockHeight(ABOVE_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE);
	}

	//endregion

	//region transitive remote harvesting

	@Test
	public void remoteHarvesterCannotActivateHisOwnRemoteHarvesterBelowLimit() {
		assertRemoteHarvesterCannotActivateHisOwnRemoteHarvester(
				new BlockHeight(BELOW_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	// two following tests, are testing following scenario
	// 1) A importance transfer to X
	// 2) send some nems to X
	// 3) X importance transfer to Y
	//
	// I'm not sure if we handle such situation properly (obviously importance transfer should not be transitive)
	@Test
	public void remoteHarvesterCannotActivateHisOwnRemoteHarvesterAboveLimit() {
		assertRemoteHarvesterCannotActivateHisOwnRemoteHarvester(
				new BlockHeight(ABOVE_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	private static void assertRemoteHarvesterCannotActivateHisOwnRemoteHarvester(
			final BlockHeight height,
			final ValidationResult validationResult) {
		final TestContext context = new TestContext();

		// - use a dummy transaction to set the state of the remote account
		final ImportanceTransferTransaction dummy = context.createTransaction(ImportanceTransferMode.Activate);
		context.setLesseeRemoteState(dummy, TEST_HEIGHT, ImportanceTransferMode.Activate);

		// - create another transaction around the dummy remote account set up previously
		final Account furtherRemote = Utils.generateRandomAccount();
		final Account remote = dummy.getRemote();
		final ImportanceTransferTransaction transaction = new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				remote,
				ImportanceTransferMode.Activate,
				furtherRemote);
		context.addRemoteLinks(furtherRemote);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(height.getRaw() + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validate(transaction, testedHeight);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	private static void assertRemoteIsOccupiedTest(
			final ImportanceTransferMode previous,
			final ImportanceTransferMode mode,
			final BlockHeight blockHeight,
			final ValidationResult expectedValidationResult) {
		// Arrange:
		final TestContext context = new TestContext();

		// - use a dummy transaction to set the state of the remote account
		final ImportanceTransferTransaction dummy = context.createTransaction(ImportanceTransferMode.Activate);
		context.setLesseeRemoteState(dummy, TEST_HEIGHT, previous);

		// - create another transaction around the dummy remote account set up previously
		final ImportanceTransferTransaction transaction = context.createTransactionWithRemote(dummy.getRemote(), mode);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(blockHeight.getRaw() + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validate(transaction, testedHeight);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	//endregion

	private static class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final ImportanceTransferTransactionValidator validator = new ImportanceTransferTransactionValidator(this.accountStateCache);

		private ImportanceTransferTransaction createTransaction(final ImportanceTransferMode mode) {
			final Account signer = Utils.generateRandomAccount();
			final Account remote = Utils.generateRandomAccount();
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
			final AccountState state = new AccountState(address);
			Mockito.when(this.accountStateCache.findStateByAddress(address))
					.thenReturn(state);
		}

		private void setLessorRemoteState(final ImportanceTransferTransaction account, final BlockHeight height, final ImportanceTransferMode mode) {
			final Address sender = account.getSigner().getAddress();
			final Address remote = account.getRemote().getAddress();
			final RemoteLink link = new RemoteLink(remote, height, mode, RemoteLink.Owner.HarvestingRemotely);
			this.accountStateCache.findStateByAddress(sender).getRemoteLinks().addLink(link);
		}

		private void setLesseeRemoteState(final ImportanceTransferTransaction account, final BlockHeight height, final ImportanceTransferMode mode) {
			final Address sender = account.getSigner().getAddress();
			final Address remote = account.getRemote().getAddress();
			final RemoteLink link = new RemoteLink(sender, height, mode, RemoteLink.Owner.RemoteHarvester);
			this.accountStateCache.findStateByAddress(remote).getRemoteLinks().addLink(link);
		}

		private ImportanceTransferTransaction createTransactionWithRemote(final Account remote, final ImportanceTransferMode mode) {
			final Account signer = Utils.generateRandomAccount();
			this.addRemoteLinks(signer);
			return new ImportanceTransferTransaction(
					TimeInstant.ZERO,
					signer,
					mode,
					remote);
		}

		private AccountInfo getAccountInfo(final Account account) {
			return this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo();
		}

		private AccountState getAccountState(final Account account) {
			return this.accountStateCache.findStateByAddress(account.getAddress());
		}

		private ValidationResult validate(final ImportanceTransferTransaction transaction) {
			return this.validate(transaction, TEST_HEIGHT);
		}

		private ValidationResult validate(final ImportanceTransferTransaction transaction, final BlockHeight testHeight) {
			return this.validator.validate(transaction, new ValidationContext(testHeight, ValidationStates.Throw));
		}
	}
}