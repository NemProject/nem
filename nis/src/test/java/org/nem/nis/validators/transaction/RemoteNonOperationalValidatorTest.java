package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

@RunWith(Enclosed.class)
public class RemoteNonOperationalValidatorTest {

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
		public void transferContainingRemoteAccountAsSignerIsNotValid() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = this.createTransfer(context.remoteAccount, context.otherAccount1);

			// Act:
			context.assertValidationResult(transaction, ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE);
		}

		protected abstract Transaction createTransfer(final Account signer, final Account other);
	}

	//region non importance transfer

	public static class NonImportanceTransferValidationTest extends DefaultTransactionValidationTest {

		@Test
		public void transferContainingRemoteAccountAsOtherIsNotValid() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = this.createTransfer(context.otherAccount1, context.remoteAccount);

			// Act:
			context.assertValidationResult(transaction, ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE);
		}

		@Override
		protected Transaction createTransfer(final Account signer, final Account other) {
			return new TransferTransaction(
					TimeInstant.ZERO,
					signer,
					other,
					Amount.fromNem(100),
					null);
		}
	}

	//endregion

	//region importance transfer

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
			return new ImportanceTransferTransaction(
					TimeInstant.ZERO,
					signer,
					ImportanceTransferMode.Activate,
					other);
		}
	}

	//endregion

	private static class TestContext {
		private final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final Account ownerAccount = Utils.generateRandomAccount();
		private final Account remoteAccount = Utils.generateRandomAccount();
		private final Account otherAccount1 = Utils.generateRandomAccount();
		private final Account otherAccount2 = Utils.generateRandomAccount();

		public TestContext() {
			Mockito.when(this.accountStateCache.findStateByAddress(Mockito.any()))
					.thenAnswer(invocationOnMock -> new AccountState((Address)invocationOnMock.getArguments()[0]));

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

		public void assertValidationResult(final Transaction transaction, final ValidationResult expectedResult) {
			// Arrange:
			final SingleTransactionValidator validator = new RemoteNonOperationalValidator(this.accountStateCache);

			// Act:
			final ValidationResult result = validator.validate(transaction, new ValidationContext(ValidationStates.Throw));

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		}
	}
}