package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

import java.util.Arrays;

@RunWith(Enclosed.class)
public class RemoteNonOperationalValidatorTest {
	private static final long FORK_HEIGHT_MOSAIC_REDEFINITION = new BlockHeight(
			BlockMarkerConstants.MOSAIC_REDEFINITION_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24)).getRaw();
	private static final long[] HEIGHTS_BEFORE_FORK = new long[]{
			1, 10, 100, 1000, FORK_HEIGHT_MOSAIC_REDEFINITION - 1
	};
	private static final long[] HEIGHTS_AT_AND_AFTER_FORK = new long[]{
			FORK_HEIGHT_MOSAIC_REDEFINITION, FORK_HEIGHT_MOSAIC_REDEFINITION + 1, FORK_HEIGHT_MOSAIC_REDEFINITION + 10,
			FORK_HEIGHT_MOSAIC_REDEFINITION + 100000
	};

	private static abstract class DefaultTransactionValidationTest {

		@Test
		public void transferContainingNoRemoteAccountIsValid() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = this.createTransfer(context.otherAccount1, context.otherAccount2);

			// Act:
			context.assertValidationResult(transaction, ValidationResult.SUCCESS);
		}

		@Test
		public void transferContainingOwnerAccountAsOtherIsValid() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = this.createTransfer(context.otherAccount1, context.ownerAccount);

			// Act:
			context.assertValidationResult(transaction, ValidationResult.SUCCESS);
		}

		@Test
		public void transferContainingOwnerAccountAsSignerIsValid() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = this.createTransfer(context.ownerAccount, context.otherAccount1);

			// Act:
			context.assertValidationResult(transaction, ValidationResult.SUCCESS);
		}

		@Test
		public void transferContainingDeactivatingRemoteAccountAsSignerIsNotValid() {
			// Arrange:
			final TestContext context = new TestContext();
			context.deactivateRemote(context.ownerAccount.getAddress(), context.remoteAccount.getAddress());
			final Transaction transaction = this.createTransfer(context.remoteAccount, context.otherAccount1);

			// Act:
			context.assertValidationResult(transaction, new BlockHeight(100), ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE);
		}

		@Test
		public void transferContainingActiveRemoteAccountAsSignerIsNotValid() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = this.createTransfer(context.remoteAccount, context.otherAccount1);

			// Act:
			context.assertValidationResult(transaction, ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE);
		}

		// region mosaic redefinition fork

		@Test
		public void transferContainingInactiveRemoteAccountAsSignerIsNotValidBeforeFork() {
			// Arrange:
			final TestContext context = new TestContext();
			context.deactivateRemote(context.ownerAccount.getAddress(), context.remoteAccount.getAddress());
			final Transaction transaction = this.createTransfer(context.remoteAccount, context.otherAccount1);

			// Act:
			Arrays.stream(HEIGHTS_BEFORE_FORK).forEach(height -> context.assertValidationResult(transaction, new BlockHeight(height),
					ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE));
		}

		@Test
		public void transferContainingInactiveRemoteAccountAsSignerIsValidAtAndAfterFork() {
			// Arrange:
			final TestContext context = new TestContext();
			context.deactivateRemote(context.ownerAccount.getAddress(), context.remoteAccount.getAddress());
			final Transaction transaction = this.createTransfer(context.remoteAccount, context.otherAccount1);

			// Act:
			Arrays.stream(HEIGHTS_AT_AND_AFTER_FORK)
					.forEach(height -> context.assertValidationResult(transaction, new BlockHeight(height), ValidationResult.SUCCESS));
		}

		// endregion

		protected abstract Transaction createTransfer(final Account signer, final Account other);
	}

	// region non importance transfer

	public static class NonImportanceTransferValidationTest extends DefaultTransactionValidationTest {

		@Test
		public void transferContainingDeactivatingRemoteAccountAsOtherIsNotValid() {
			// Arrange:
			final TestContext context = new TestContext();
			context.deactivateRemote(context.ownerAccount.getAddress(), context.remoteAccount.getAddress());
			final Transaction transaction = this.createTransfer(context.otherAccount1, context.remoteAccount);

			// Act:
			context.assertValidationResult(transaction, new BlockHeight(100), ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE);
		}

		@Test
		public void transferContainingActiveRemoteAccountAsOtherIsNotValid() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = this.createTransfer(context.otherAccount1, context.remoteAccount);

			// Act:
			context.assertValidationResult(transaction, ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE);
		}

		// region mosaic redefinition fork

		@Test
		public void transferContainingInactiveRemoteAccountAsOtherIsNotValidBeforeFork() {
			// Arrange:
			final TestContext context = new TestContext();
			context.deactivateRemote(context.ownerAccount.getAddress(), context.remoteAccount.getAddress());
			final Transaction transaction = this.createTransfer(context.otherAccount1, context.remoteAccount);

			// Act:
			Arrays.stream(HEIGHTS_BEFORE_FORK).forEach(height -> context.assertValidationResult(transaction, new BlockHeight(height),
					ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE));
		}

		@Test
		public void transferContainingInactiveRemoteAccountAsOtherIsValidAtAndAfterFork() {
			// Arrange:
			final TestContext context = new TestContext();
			context.deactivateRemote(context.ownerAccount.getAddress(), context.remoteAccount.getAddress());
			final Transaction transaction = this.createTransfer(context.otherAccount1, context.remoteAccount);

			// Act:
			Arrays.stream(HEIGHTS_AT_AND_AFTER_FORK)
					.forEach(height -> context.assertValidationResult(transaction, new BlockHeight(height), ValidationResult.SUCCESS));
		}

		// endregion

		@Override
		protected Transaction createTransfer(final Account signer, final Account other) {
			return new TransferTransaction(TimeInstant.ZERO, signer, other, Amount.fromNem(100), null);
		}
	}

	// endregion

	// region importance transfer

	public static class ImportanceTransferValidationTest extends DefaultTransactionValidationTest {

		@Test
		public void transferContainingRemoteAccountAsOtherIsValid() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = this.createTransfer(context.otherAccount1, context.remoteAccount);

			// Act:
			context.assertValidationResult(transaction, ValidationResult.SUCCESS);
		}

		@Override
		protected Transaction createTransfer(final Account signer, final Account other) {
			return new ImportanceTransferTransaction(TimeInstant.ZERO, signer, ImportanceTransferMode.Activate, other);
		}
	}

	// endregion

	private static class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final Account ownerAccount = Utils.generateRandomAccount();
		private final Account remoteAccount = Utils.generateRandomAccount();
		private final Account otherAccount1 = Utils.generateRandomAccount();
		private final Account otherAccount2 = Utils.generateRandomAccount();

		public TestContext() {
			Mockito.when(this.accountStateCache.findStateByAddress(Mockito.any()))
					.thenAnswer(invocationOnMock -> new AccountState((Address) invocationOnMock.getArguments()[0]));

			this.setupRemote(this.ownerAccount.getAddress(), this.remoteAccount.getAddress());
		}

		private void setupRemote(final Address ownerAddress, final Address remoteAddress) {
			final ImportanceTransferMode mode = ImportanceTransferMode.Activate;

			final AccountState ownerAccountState = new AccountState(remoteAddress);

			ownerAccountState.getRemoteLinks()
					.addLink(new RemoteLink(remoteAddress, BlockHeight.ONE, mode, RemoteLink.Owner.HarvestingRemotely));
			Mockito.when(this.accountStateCache.findStateByAddress(ownerAddress)).thenReturn(ownerAccountState);

			final AccountState remoteAccountState = new AccountState(remoteAddress);
			remoteAccountState.getRemoteLinks()
					.addLink(new RemoteLink(ownerAddress, BlockHeight.ONE, mode, RemoteLink.Owner.RemoteHarvester));
			Mockito.when(this.accountStateCache.findStateByAddress(remoteAddress)).thenReturn(remoteAccountState);
		}

		private void deactivateRemote(final Address ownerAddress, final Address remoteAddress) {
			final AccountState ownerAccountState = this.accountStateCache.findStateByAddress(ownerAddress);
			ownerAccountState.getRemoteLinks().addLink(
					new RemoteLink(remoteAddress, BlockHeight.ONE, ImportanceTransferMode.Deactivate, RemoteLink.Owner.HarvestingRemotely));

			final AccountState remoteAccountState = this.accountStateCache.findStateByAddress(remoteAddress);
			remoteAccountState.getRemoteLinks().addLink(
					new RemoteLink(ownerAddress, BlockHeight.ONE, ImportanceTransferMode.Deactivate, RemoteLink.Owner.RemoteHarvester));
		}

		public void assertValidationResult(final Transaction transaction, final ValidationResult expectedResult) {
			assertValidationResult(transaction, new BlockHeight(50), expectedResult);
		}

		public void assertValidationResult(final Transaction transaction, final BlockHeight height, final ValidationResult expectedResult) {
			// Arrange:
			final SingleTransactionValidator validator = new RemoteNonOperationalValidator(this.accountStateCache);

			// Act:
			final ValidationResult result = validator.validate(transaction, new ValidationContext(height, ValidationStates.Throw));

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
		}
	}
}
