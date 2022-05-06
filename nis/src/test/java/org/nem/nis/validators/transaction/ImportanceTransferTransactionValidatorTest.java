package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.ValidationContext;

import java.util.Collections;
import java.util.function.BiConsumer;

public class ImportanceTransferTransactionValidatorTest {
	private static final BlockHeight TEST_HEIGHT = new BlockHeight(123);
	private static final long FORK_HEIGHT_REMOTE_ACCOUNT = new BlockHeight(
			BlockMarkerConstants.REMOTE_ACCOUNT_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24)).getRaw();
	private static final int ABOVE_LIMIT = NisTestConstants.REMOTE_HARVESTING_DELAY;
	private static final int BELOW_LIMIT = ABOVE_LIMIT - 1;
	private static final long[] HEIGHTS_BEFORE_FORK = new long[]{
			1, 10, 100, FORK_HEIGHT_REMOTE_ACCOUNT - 1
	};
	private static final long[] HEIGHTS_AT_AND_AFTER_FORK = new long[]{
			FORK_HEIGHT_REMOTE_ACCOUNT, FORK_HEIGHT_REMOTE_ACCOUNT + 1, FORK_HEIGHT_REMOTE_ACCOUNT + 100
	};

	// region first link

	@Test
	public void activateImportanceTransferIsValidAsFirstLink() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(ImportanceTransferMode.Activate);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void deactivateImportanceTransferIsNotValidAsFirstLink() {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(ImportanceTransferMode.Deactivate);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE));
	}

	// endregion

	// region account is acceptable as remote validation

	@Test
	public void activateImportanceTransferIsInvalidWhenRecipientHasBalance() {
		// Arrange:
		final long[] heights = new long[]{
				1, FORK_HEIGHT_REMOTE_ACCOUNT - 1, FORK_HEIGHT_REMOTE_ACCOUNT, FORK_HEIGHT_REMOTE_ACCOUNT + 1,
				FORK_HEIGHT_REMOTE_ACCOUNT + 100
		};

		// Assert:
		assertImportanceTransferTransactionValidation((c, t) -> c.getAccountInfo(t.getRemote()).incrementBalance(Amount.fromNem(1)),
				heights, ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE);
	}

	// endregion

	// region Remote Account Fork related - before fork

	@Test
	public void activateImportanceTransferIsValidWhenRemoteOwnsMosaicBeforeFork() {
		// Assert: remote account owns a mosaic
		assertImportanceTransferTransactionValidation((c, t) -> c.getAccountInfo(t.getRemote()).addMosaicId(Utils.createMosaicId(123)),
				HEIGHTS_BEFORE_FORK, ValidationResult.SUCCESS);
	}

	@Test
	public void activateImportanceTransferIsValidWhenRemoteOwnsNamespaceBeforeFork() {
		// Assert: remote account owns a namespace
		assertImportanceTransferTransactionValidation((c, t) -> c.addNamespaceOwner(t.getRemote(), new NamespaceId("foo")),
				HEIGHTS_BEFORE_FORK, ValidationResult.SUCCESS);
	}

	@Test
	public void activateImportanceTransferIsValidWhenRemoteIsMultisigBeforeFork() {
		// Assert: remote account is multisig account
		assertImportanceTransferTransactionValidation(
				(c, t) -> c.getAccountState(t.getRemote()).getMultisigLinks().addCosignatory(Utils.generateRandomAddress()),
				HEIGHTS_BEFORE_FORK, ValidationResult.SUCCESS);
	}

	@Test
	public void activateImportanceTransferIsValidWhenRemoteIsCosignatoryBeforeFork() {
		// Assert: remote account is cosignatory
		assertImportanceTransferTransactionValidation(
				(c, t) -> c.getAccountState(t.getRemote()).getMultisigLinks().addCosignatoryOf(Utils.generateRandomAddress()),
				HEIGHTS_BEFORE_FORK, ValidationResult.SUCCESS);
	}

	// endregion

	// region Remote Account Fork related - after fork

	@Test
	public void activateImportanceTransferIsInvalidWhenRemoteOwnsMosaicAtAndAfterFork() {
		// Assert: remote account owns a mosaic
		assertImportanceTransferTransactionValidation((c, t) -> c.getAccountInfo(t.getRemote()).addMosaicId(Utils.createMosaicId(123)),
				HEIGHTS_AT_AND_AFTER_FORK, ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE);
	}

	@Test
	public void activateImportanceTransferIsInvalidWhenRemoteOwnsNamespaceAtAndAfterFork() {
		// Assert: remote account owns a namespace
		assertImportanceTransferTransactionValidation((c, t) -> c.addNamespaceOwner(t.getRemote(), new NamespaceId("foo")),
				HEIGHTS_AT_AND_AFTER_FORK, ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE);
	}

	@Test
	public void activateImportanceTransferIsInvalidWhenRemoteIsMultisigAtAndAfterFork() {
		// Assert: remote account is multisig account
		assertImportanceTransferTransactionValidation(
				(c, t) -> c.getAccountState(t.getRemote()).getMultisigLinks().addCosignatory(Utils.generateRandomAddress()),
				HEIGHTS_AT_AND_AFTER_FORK, ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE);
	}

	@Test
	public void activateImportanceTransferIsInvalidWhenRemoteIsCosignatoryAtAndAfterFork() {
		// Assert: remote account is cosignatory
		assertImportanceTransferTransactionValidation(
				(c, t) -> c.getAccountState(t.getRemote()).getMultisigLinks().addCosignatoryOf(Utils.generateRandomAddress()),
				HEIGHTS_AT_AND_AFTER_FORK, ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE);
	}

	// endregion

	private static void assertImportanceTransferTransactionValidation(
			final BiConsumer<TestContext, ImportanceTransferTransaction> contextSetup, final long[] heights,
			final ValidationResult expectedResult) {
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(ImportanceTransferMode.Activate);
		contextSetup.accept(context, transaction);

		for (final long height : heights) {
			// Act:
			final ValidationResult result = context.validate(transaction, new BlockHeight(height));

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
		}
	}

	// region one day after opposite link

	@Test
	public void activateImportanceTransferIsValidOneDayAfterDeactivateLink() {
		// Assert:
		assertTransferIsValidOneDayAfterOppositeLink(ImportanceTransferMode.Deactivate, ImportanceTransferMode.Activate);
	}

	@Test
	public void deactivateImportanceTransferIsValidOneDayAfterActivateLink() {
		// Assert:
		assertTransferIsValidOneDayAfterOppositeLink(ImportanceTransferMode.Activate, ImportanceTransferMode.Deactivate);
	}

	private static void assertTransferIsValidOneDayAfterOppositeLink(final ImportanceTransferMode initialLink,
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
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	// endregion

	// region less than one day after opposite link

	@Test
	public void activateImportanceTransferIsNotValidLessThanOneDayAfterDeactivateLink() {
		// Assert:
		assertTransferIsNotValidLessThanOneDayAfterOppositeLink(ImportanceTransferMode.Deactivate, ImportanceTransferMode.Activate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void deactivateImportanceTransferIsNotValidLessThanOneDayAfterActivateLink() {
		// Assert:
		assertTransferIsNotValidLessThanOneDayAfterOppositeLink(ImportanceTransferMode.Activate, ImportanceTransferMode.Deactivate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	private static void assertTransferIsNotValidLessThanOneDayAfterOppositeLink(final ImportanceTransferMode initialLink,
			final ImportanceTransferMode newLink, final ValidationResult expectedValidationResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final ImportanceTransferTransaction transaction = context.createTransaction(newLink);
		context.setLessorRemoteState(transaction, TEST_HEIGHT, initialLink);
		context.setLesseeRemoteState(transaction, TEST_HEIGHT, initialLink);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(BELOW_LIMIT + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validate(transaction, testedHeight);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	// endregion

	// region after same link

	@Test
	public void activateImportanceTransferIsNotValidAfterActivateLink() {
		// Assert:
		assertTransferIsNotValidAfterSameLink(ImportanceTransferMode.Activate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void deactivateImportanceTransferIsNotValidAfterDeactivateLink() {
		// Assert:
		assertTransferIsNotValidAfterSameLink(ImportanceTransferMode.Deactivate,
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE);
	}

	private static void assertTransferIsNotValidAfterSameLink(final ImportanceTransferMode mode,
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
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	// endregion

	// region remote is already occupied

	@Test
	public void cannotActivateIfRemoteIsActiveBelowLimit() {
		assertRemoteIsOccupiedTest(ImportanceTransferMode.Activate, ImportanceTransferMode.Activate, new BlockHeight(BELOW_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotActivateIfRemoteIsActiveAboveLimit() {
		assertRemoteIsOccupiedTest(ImportanceTransferMode.Activate, ImportanceTransferMode.Activate, new BlockHeight(ABOVE_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void cannotActivateIfRemoteIsDeactivatedBelowLimit() {
		assertRemoteIsOccupiedTest(ImportanceTransferMode.Deactivate, ImportanceTransferMode.Activate, new BlockHeight(BELOW_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void canActivateIfRemoteIsDeactivatedAboveLimit() {
		assertRemoteIsOccupiedTest(ImportanceTransferMode.Deactivate, ImportanceTransferMode.Activate, new BlockHeight(ABOVE_LIMIT),
				ValidationResult.SUCCESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsActiveBelowLimit() {
		assertRemoteIsOccupiedTest(ImportanceTransferMode.Activate, ImportanceTransferMode.Deactivate, new BlockHeight(BELOW_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsActiveAboveLimit() {
		assertRemoteIsOccupiedTest(ImportanceTransferMode.Activate, ImportanceTransferMode.Deactivate, new BlockHeight(ABOVE_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	@Test
	public void cannotDeactivateIfRemoteIsDeactivatedBelowLimit() {
		assertRemoteIsOccupiedTest(ImportanceTransferMode.Deactivate, ImportanceTransferMode.Deactivate, new BlockHeight(BELOW_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotDeactivateIfRemoteIsDeactivatedAboveLimit() {
		// note that this will actually fail in validateOwner not validateRemote
		assertRemoteIsOccupiedTest(ImportanceTransferMode.Deactivate, ImportanceTransferMode.Deactivate, new BlockHeight(ABOVE_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE);
	}

	// endregion

	// region transitive remote harvesting

	@Test
	public void remoteHarvesterCannotActivateHisOwnRemoteHarvesterBelowLimit() {
		assertRemoteHarvesterCannotActivateHisOwnRemoteHarvester(new BlockHeight(BELOW_LIMIT),
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
		assertRemoteHarvesterCannotActivateHisOwnRemoteHarvester(new BlockHeight(ABOVE_LIMIT),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED);
	}

	private static void assertRemoteHarvesterCannotActivateHisOwnRemoteHarvester(final BlockHeight height,
			final ValidationResult validationResult) {
		final TestContext context = new TestContext();

		// - use a dummy transaction to set the state of the remote account
		final ImportanceTransferTransaction dummy = context.createTransaction(ImportanceTransferMode.Activate);
		context.setLesseeRemoteState(dummy, TEST_HEIGHT, ImportanceTransferMode.Activate);

		// - create another transaction around the dummy remote account set up previously
		final Account furtherRemote = Utils.generateRandomAccount();
		final Account remote = dummy.getRemote();
		final ImportanceTransferTransaction transaction = new ImportanceTransferTransaction(TimeInstant.ZERO, remote,
				ImportanceTransferMode.Activate, furtherRemote);
		context.addRemoteLinks(furtherRemote);

		// Act:
		final BlockHeight testedHeight = new BlockHeight(height.getRaw() + TEST_HEIGHT.getRaw());
		final ValidationResult result = context.validate(transaction, testedHeight);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	private static void assertRemoteIsOccupiedTest(final ImportanceTransferMode previous, final ImportanceTransferMode mode,
			final BlockHeight blockHeight, final ValidationResult expectedValidationResult) {
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
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	// endregion

	private static class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final NamespaceCache namespaceCache = Mockito.mock(NamespaceCache.class);
		private final ImportanceTransferTransactionValidator validator = new ImportanceTransferTransactionValidator(this.accountStateCache,
				this.namespaceCache);

		private ImportanceTransferTransaction createTransaction(final ImportanceTransferMode mode) {
			final Account signer = Utils.generateRandomAccount();
			final Account remote = Utils.generateRandomAccount();
			this.addRemoteLinks(signer);
			this.addRemoteLinks(remote);
			return new ImportanceTransferTransaction(TimeInstant.ZERO, signer, mode, remote);
		}

		private void addRemoteLinks(final Account account) {
			final Address address = account.getAddress();
			final AccountState state = new AccountState(address);
			Mockito.when(this.accountStateCache.findStateByAddress(address)).thenReturn(state);
		}

		private void setLessorRemoteState(final ImportanceTransferTransaction account, final BlockHeight height,
				final ImportanceTransferMode mode) {
			final Address sender = account.getSigner().getAddress();
			final Address remote = account.getRemote().getAddress();
			final RemoteLink link = new RemoteLink(remote, height, mode, RemoteLink.Owner.HarvestingRemotely);
			this.accountStateCache.findStateByAddress(sender).getRemoteLinks().addLink(link);
		}

		private void setLesseeRemoteState(final ImportanceTransferTransaction account, final BlockHeight height,
				final ImportanceTransferMode mode) {
			final Address sender = account.getSigner().getAddress();
			final Address remote = account.getRemote().getAddress();
			final RemoteLink link = new RemoteLink(sender, height, mode, RemoteLink.Owner.RemoteHarvester);
			this.accountStateCache.findStateByAddress(remote).getRemoteLinks().addLink(link);
		}

		private void addNamespaceOwner(final Account account, final NamespaceId id) {
			final Namespace namespace = new Namespace(id, account, new BlockHeight(123));
			final NamespaceEntry entry = new NamespaceEntry(namespace, new Mosaics(id));
			Mockito.when(this.namespaceCache.getRootNamespaceIds()).thenReturn(Collections.singletonList(id));
			Mockito.when(this.namespaceCache.get(id)).thenReturn(entry);
		}

		private ImportanceTransferTransaction createTransactionWithRemote(final Account remote, final ImportanceTransferMode mode) {
			final Account signer = Utils.generateRandomAccount();
			this.addRemoteLinks(signer);
			return new ImportanceTransferTransaction(TimeInstant.ZERO, signer, mode, remote);
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
