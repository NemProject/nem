package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

import java.util.Collection;
import java.util.stream.Collectors;

public class MinimumFeeValidatorTest {

	@Test
	public void transactionWithInvalidFeeFailsValidation() {
		// Assert:
		assertValidationResultForMockTransactionFee(
				MockTransaction.DEFAULT_FEE.subtract(Amount.fromNem(1)),
				ValidationResult.FAILURE_INSUFFICIENT_FEE);
	}

	@Test
	public void transactionWithValidFeePassesValidation() {
		// Assert:
		assertValidationResultForMockTransactionFee(
				MockTransaction.DEFAULT_FEE,
				ValidationResult.SUCCESS);
	}

	private static void assertValidationResultForMockTransactionFee(final Amount fee, final ValidationResult expectedResult) {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setFee(fee);

		// Act:
		final ValidationResult result = validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	@Test
	public void transferTransactionIncludingMosaicsWithInvalidFeeFailsValidation() {
		// Assert:
		assertValidationResultForTransferTransactionWithMosaicFee(Amount.fromNem(192), ValidationResult.FAILURE_INSUFFICIENT_FEE);
	}

	@Test
	public void transferTransactionIncludingMosaicsWithValidFeePassesValidation() {
		// Assert:
		assertValidationResultForTransferTransactionWithMosaicFee(Amount.fromNem(193), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionValidationWithNonZeroFeesPassesValidationIfIgnoreFeesIsSet() {
		assertValidationPassesForFee(Amount.fromNem(1234));
	}

	@Test
	public void transactionValidationWithZeroFeesPassesValidationIfIgnoreFeesIsSet() {
		assertValidationPassesForFee(Amount.ZERO);
	}

	private static void assertValidationPassesForFee(final Amount fee) {
		// Arrange:
		final SingleTransactionValidator validator = new MinimumFeeValidator(new DefaultNamespaceCache(), true);
		final Collection<Transaction> transactions = TestTransactionRegistry.stream()
				.map(entry -> {
					final Transaction transaction = entry.createModel.get();
					transaction.setFee(fee);
					return transaction;
				})
				.collect(Collectors.toList());

		// Test a transaction with mosaics and message too
		final Message message = new PlainMessage("Hi there".getBytes());
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment(message);
		attachment.addMosaic(Utils.createMosaicId(1), new Quantity(12));
		final TransferTransaction transaction = RandomTransactionFactory.createTransferWithAttachment(attachment);
		transaction.setFee(fee);
		transactions.add(transaction);

		// Assert:
		transactions.forEach(t -> Assert.assertThat(
				validator.validate(t, new ValidationContext(ValidationStates.Throw)),
				IsEqual.equalTo(ValidationResult.SUCCESS)));
	}

	private static void assertValidationResultForTransferTransactionWithMosaicFee(final Amount fee, final ValidationResult expectedResult) {
		// Arrange:
		final Account namespaceOwner = Utils.generateRandomAccount();
		final MosaicId mosaicId = Utils.createMosaicId(1);
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(namespaceOwner, mosaicId, Utils.createMosaicProperties(10000L, 0, null, null));
		final NamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		namespaceCache.add(new Namespace(mosaicId.getNamespaceId(), namespaceOwner, BlockHeight.ONE));
		namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().add(mosaicDefinition);

		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaicId, new Quantity(1000));
		final TransferTransaction transaction = new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(1),
				attachment);
		transaction.setFee(fee);
		transaction.setDeadline(new TimeInstant(1));

		final SingleTransactionValidator validator = new MinimumFeeValidator(namespaceCache, false);

		// Act:
		final ValidationResult result = validator.validate(transaction, new ValidationContext(new BlockHeight(511000), ValidationStates.Throw));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static ValidationResult validate(final Transaction transaction) {
		// Arrange:
		final SingleTransactionValidator validator = new MinimumFeeValidator(new DefaultNamespaceCache(), false);

		// Act:
		return validator.validate(transaction, new ValidationContext(new BlockHeight(511000), ValidationStates.Throw));
	}
}