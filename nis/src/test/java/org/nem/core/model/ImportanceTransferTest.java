package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class ImportanceTransferTest {
	private static final TimeInstant TIME = new TimeInstant(123);

	//region Constructor

	@Test(expected = IllegalArgumentException.class)
	public void remoteIsRequired() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		this.createImportanceTransferTransaction(signer, ImportanceTransferDirection.Transfer, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void properModeIsRequired() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		// Act:
		this.createImportanceTransferTransaction(signer, 666, remote);
	}

	@Test
	public void ctorCanCreateImportanceTransfer() {
		assertCtorCanCreateImportanceTransfer(ImportanceTransferDirection.Transfer);
	}

	@Test
	public void ctorCanCreateImportanceTransferRevert() {
		assertCtorCanCreateImportanceTransfer(ImportanceTransferDirection.Revert);
	}

	private void assertCtorCanCreateImportanceTransfer(int direction) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		// Act:
		final ImportanceTransfer importanceTransfer = this.createImportanceTransferTransaction(signer, direction, remote);

		// Assert:
		Assert.assertThat(importanceTransfer.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(importanceTransfer.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(importanceTransfer.getRemote(), IsEqual.equalTo(remote));
		Assert.assertThat(importanceTransfer.getDirection(), IsEqual.equalTo(direction));
	}


	private ImportanceTransfer createImportanceTransferTransaction(final Account sender, int mode, final Account remote) {
		return new ImportanceTransfer(TIME, sender, mode, remote);
	}

	// endregion

	// region roundtrip
	@Test
	public void canRoundTripImportanceTransfer()  {
		assertImportanceTransferCanBeRoundTripped(ImportanceTransferDirection.Transfer);
	}

	@Test
	public void canRoundTripImportanceTransferRevert()  {
		assertImportanceTransferCanBeRoundTripped(ImportanceTransferDirection.Revert);
	}

	public void assertImportanceTransferCanBeRoundTripped(int direction) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		final ImportanceTransfer originalTransaction = this.createImportanceTransferTransaction(signer, direction, remote);

		// Act:
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, remote);
		final ImportanceTransfer importanceTransfer = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		Assert.assertThat(importanceTransfer.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(importanceTransfer.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(importanceTransfer.getRemote(), IsEqual.equalTo(remote));
		Assert.assertThat(importanceTransfer.getDirection(), IsEqual.equalTo(direction));
	}

	private ImportanceTransfer createRoundTrippedTransaction(
			final ImportanceTransfer originalTransaction,
			final AccountLookup accountLookup) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");
		return new ImportanceTransfer(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}
	// endregion

	// region validate
	@Test
	public void canValidateImportanceTransfer() {
		assertValidateImportanceTransfer(1, ValidationResult.SUCCESS);
	}

	@Test
	public void cantValidateImportanceTransferWithoutNems() {
		assertValidateImportanceTransfer(0, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	public void assertValidateImportanceTransfer(final int amount, final ValidationResult result) {
		// Arrange:
		final int direction = ImportanceTransferDirection.Transfer;
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		final ImportanceTransfer transaction = this.createImportanceTransferTransaction(signer, direction, remote);
		transaction.setDeadline(transaction.getTimeStamp().addHours(1));
		signer.incrementBalance(Amount.fromNem(amount));

		// Act + Assert:
		Assert.assertThat(transaction.checkValidity(), IsEqual.equalTo(result));
	}
	// endregion

	// region execute
	@Test
	public void executeTransfersFeeFromSigner() {
		// Arrange:
		final int direction = ImportanceTransferDirection.Transfer;
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(100));
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransfer transaction = this.createImportanceTransferTransaction(signer, direction, remote);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		transaction.execute();

		// Assert:
		Assert.assertThat(signer.getBalance(), IsEqual.equalTo(Amount.fromNem(90L)));
	}
	// endregion

	// region undo
	@Test
	public void undoTransfersFeeFromSigner() {
		// Arrange:
		final int direction = ImportanceTransferDirection.Transfer;
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(90));
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransfer transaction = this.createImportanceTransferTransaction(signer, direction, remote);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		transaction.undo();

		// Assert:
		Assert.assertThat(signer.getBalance(), IsEqual.equalTo(Amount.fromNem(100L)));
	}
	// endregion
}
