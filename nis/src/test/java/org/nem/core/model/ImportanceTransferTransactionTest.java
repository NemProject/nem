package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class ImportanceTransferTransactionTest {
	private static final TimeInstant TIME = new TimeInstant(123);

	//region Constructor

	@Test(expected = IllegalArgumentException.class)
	public void remoteIsRequired() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		this.createImportanceTransferTransaction(signer, ImportanceTransferTransactionDirection.Transfer, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void properModeIsRequired() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		// Act:
		this.createImportanceTransferTransaction(signer, 666, remote);
	}

	@Test(expected = SerializationException.class)
	public void deserializationFailsWhenAddressIsMissing() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, remote);

		final ImportanceTransferTransaction originalEntity = this.createImportanceTransferTransaction(signer, ImportanceTransferTransactionDirection.Transfer, remote);
		originalEntity.sign();
		final JsonSerializer jsonSerializer = new JsonSerializer(true);
		originalEntity.serialize(jsonSerializer);
		final JSONObject jsonObject = jsonSerializer.getObject();

		// Act:
		jsonObject.put("remoteAddress", null);
		final Deserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(accountLookup));
		deserializer.readInt("type");
		new ImportanceTransferTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}

	@Test
	public void ctorCanCreateImportanceTransfer() {
		assertCtorCanCreateImportanceTransfer(ImportanceTransferTransactionDirection.Transfer);
	}

	@Test
	public void ctorCanCreateImportanceTransferRevert() {
		assertCtorCanCreateImportanceTransfer(ImportanceTransferTransactionDirection.Revert);
	}

	private void assertCtorCanCreateImportanceTransfer(int direction) {
		// Arrange:
		// TODO-CR: for the remote consider using Utils.generateRandomAddress();
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		// Act:
		final ImportanceTransferTransaction importanceTransferTransaction = this.createImportanceTransferTransaction(signer, direction, remote);

		// Assert:
		Assert.assertThat(importanceTransferTransaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(importanceTransferTransaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(importanceTransferTransaction.getRemote(), IsEqual.equalTo(remote.getAddress()));
		Assert.assertThat(importanceTransferTransaction.getDirection(), IsEqual.equalTo(direction));
		Assert.assertThat(importanceTransferTransaction.getMinimumFee(), IsEqual.equalTo(Amount.fromNem(1)));
	}


	private ImportanceTransferTransaction createImportanceTransferTransaction(final Account sender, int mode, final Account remote) {
		return new ImportanceTransferTransaction(TIME, sender, mode, remote != null ? remote.getAddress() : null);
	}

	// endregion

	// region roundtrip
	@Test
	public void canRoundTripImportanceTransfer()  {
		assertImportanceTransferCanBeRoundTripped(ImportanceTransferTransactionDirection.Transfer);
	}

	@Test
	public void canRoundTripImportanceTransferRevert()  {
		assertImportanceTransferCanBeRoundTripped(ImportanceTransferTransactionDirection.Revert);
	}

	public void assertImportanceTransferCanBeRoundTripped(int direction) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		final ImportanceTransferTransaction originalTransaction = this.createImportanceTransferTransaction(signer, direction, remote);

		// Act:
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, remote);
		final ImportanceTransferTransaction importanceTransferTransaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		Assert.assertThat(importanceTransferTransaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(importanceTransferTransaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(importanceTransferTransaction.getRemote(), IsEqual.equalTo(remote.getAddress()));
		Assert.assertThat(importanceTransferTransaction.getDirection(), IsEqual.equalTo(direction));
		Assert.assertThat(importanceTransferTransaction.getMinimumFee(), IsEqual.equalTo(Amount.fromNem(1)));
	}

	private ImportanceTransferTransaction createRoundTrippedTransaction(
			final ImportanceTransferTransaction originalTransaction,
			final AccountLookup accountLookup) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");
		return new ImportanceTransferTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
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
		final int direction = ImportanceTransferTransactionDirection.Transfer;
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		final ImportanceTransferTransaction transaction = this.createImportanceTransferTransaction(signer, direction, remote);
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
		final int direction = ImportanceTransferTransactionDirection.Transfer;
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(100));
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransferTransaction transaction = this.createImportanceTransferTransaction(signer, direction, remote);
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
		final int direction = ImportanceTransferTransactionDirection.Transfer;
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(90));
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransferTransaction transaction = this.createImportanceTransferTransaction(signer, direction, remote);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		transaction.undo();

		// Assert:
		Assert.assertThat(signer.getBalance(), IsEqual.equalTo(Amount.fromNem(100L)));
	}
	// endregion
}
