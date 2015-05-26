package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

@RunWith(Enclosed.class)
public class TransactionFeeCalculatorTest {
	private static final long FEE_UNIT = 2;

	//region calculateMinimumFee

	//region transfer

	public static class TransferMinimumFeeCalculation {
		private final long SMALL_TRANSFER_PENALTY = 10;
		private final long MIN_TRANSFER_FEE = FEE_UNIT;

		@Test
		public void feeIsCalculatedCorrectlyForEmptyTransfer() {
			// Assert:
			assertFee(0, 0, Amount.fromNem(SMALL_TRANSFER_PENALTY));
		}

		@Test
		public void feeIsCalculatedCorrectlyForPenalizedSmallTransfers() {
			// Assert:
			for (int i = 1; i < 9; ++i) {
				assertFee(i, 0, Amount.fromNem(SMALL_TRANSFER_PENALTY - i));
			}
		}

		@Test
		public void feeIsCalculatedCorrectlyForNonPenalizedSmallTransfers() {
			// Assert:
			for (int i = 9; i < 20; ++i) {
				assertFee(i, 0, Amount.fromNem(MIN_TRANSFER_FEE));
			}
		}

		@Test
		public void feeIsCalculatedCorrectlyNearTransferStepIncreases() {
			// Assert:
			assertFee(4546, 0, Amount.fromNem(2));
			assertFee(4547, 0, Amount.fromNem(3));
			assertFee(4548, 0, Amount.fromNem(3));

			assertFee(6063, 0, Amount.fromNem(3));
			assertFee(6064, 0, Amount.fromNem(4));
			assertFee(6065, 0, Amount.fromNem(4));
		}

		@Test
		public void feeIsCalculatedCorrectlyForLargeTransfers() {
			// Assert:
			assertFee(10000, 0, Amount.fromNem(6));
			assertFee(100000, 0, Amount.fromNem(58));
			assertFee(1000000, 0, Amount.fromNem(140));
			assertFee(2000000, 0, Amount.fromNem(148));
		}

		@Test
		public void feeIsCalculatedCorrectlyForLargeTransfersWithMessages() {
			// Assert:
			assertFee(10000, 96, Amount.fromNem(6 + 12));
			assertFee(100000, 128, Amount.fromNem(58 + 16));
			assertFee(1000000, 96, Amount.fromNem(140 + 12));
			assertFee(2000000, 128, Amount.fromNem(148 + 16));
		}

		@Test
		public void feeIsCalculatedCorrectlyForTransferWithSmallestMessage() {
			// Assert:
			assertFee(1200, 1, Amount.fromNem(MIN_TRANSFER_FEE + 2));
		}

		@Test
		public void feeIsCalculatedCorrectlyForTransferNearMessageStepIncreases() {
			// Assert:
			assertFee(1200, 31, Amount.fromNem(MIN_TRANSFER_FEE + 2));
			assertFee(1200, 32, Amount.fromNem(MIN_TRANSFER_FEE + 4));
			assertFee(1200, 33, Amount.fromNem(MIN_TRANSFER_FEE + 4));

			assertFee(1200, 63, Amount.fromNem(MIN_TRANSFER_FEE + 6));
			assertFee(1200, 64, Amount.fromNem(MIN_TRANSFER_FEE + 8));
			assertFee(1200, 65, Amount.fromNem(MIN_TRANSFER_FEE + 8));
		}

		@Test
		public void feeIsCalculatedCorrectlyForTransferWithLargeMessage() {
			// Assert:
			assertFee(1200, 96, Amount.fromNem(MIN_TRANSFER_FEE + 12));
			assertFee(1200, 128, Amount.fromNem(MIN_TRANSFER_FEE + 16));
			assertFee(1200, 256, Amount.fromNem(MIN_TRANSFER_FEE + 32));
		}

		@Test
		public void messageFeeIsBasedOnEncodedSize() {
			// Assert:
			assertMessageFee(96, 128, Amount.fromNem(SMALL_TRANSFER_PENALTY + 12));
			assertMessageFee(128, 96, Amount.fromNem(SMALL_TRANSFER_PENALTY + 16));
		}

		private static void assertFee(final long amount, final int messageSize, final Amount expectedFee) {
			// Arrange:
			final Message message = 0 == messageSize ? null : new PlainMessage(new byte[messageSize]);
			final Transaction transaction = createTransfer(amount, message);

			// Assert:
			assertTransactionFee(transaction, expectedFee);
		}

		private static void assertMessageFee(final int encodedMessageSize, final int decodedMessageSize, final Amount expectedFee) {
			// Arrange:
			final Transaction transaction = createTransferWithMockMessage(encodedMessageSize, decodedMessageSize);

			// Assert:
			assertTransactionFee(transaction, expectedFee);
		}

		private static Transaction createTransferWithMockMessage(final int encodedMessageSize, final int decodedMessageSize) {
			// Arrange:
			final MockMessage message = new MockMessage(7);
			message.setEncodedPayload(new byte[encodedMessageSize]);
			message.setDecodedPayload(new byte[decodedMessageSize]);
			return createTransfer(0, message);
		}
	}

	//endregion

	//region multisig aggregate modification

	public static class MultisigAggregateModificationMinimumFeeCalculation {

		@Test
		public void feeIsCalculatedCorrectlyForSingleModification() {
			// Assert:
			assertFee(1, Amount.fromNem((5 + 3) * FEE_UNIT));
		}

		@Test
		public void feeIsCalculatedCorrectlyForMultipleModifications() {
			// Assert:
			for (int i = 2; i < 10; ++i) {
				assertFee(i, Amount.fromNem((5 + 3 * i) * FEE_UNIT));
			}
		}

		private static void assertFee(final int numModifications, final Amount expectedFee) {
			// Arrange:
			final Transaction transaction = createMultisigAggregateModification(numModifications);

			// Assert:
			assertTransactionFee(transaction, expectedFee);
		}
	}

	//endregion

	//region other transactions

	private static abstract class DefaultMinimumFeeCalculation {
		private static final long DEFAULT_FEE = 6;

		@Test
		public void feeIsDefaultFee() {
			// Arrange:
			final Transaction transaction = this.createTransaction();

			// Assert:
			assertTransactionFee(transaction, Amount.fromNem(DEFAULT_FEE));
		}

		protected abstract Transaction createTransaction();
	}

	public static class ImportanceTransferMinimumFeeCalculation extends DefaultMinimumFeeCalculation {

		@Override
		protected Transaction createTransaction() {
			return createImportanceTransfer();
		}
	}

	public static class MultisigMinimumFeeCalculation extends DefaultMinimumFeeCalculation {

		@Override
		protected Transaction createTransaction() {
			return createMultisig();
		}
	}

	public static class MultisigSignatureMinimumFeeCalculation extends DefaultMinimumFeeCalculation {

		@Override
		protected Transaction createTransaction() {
			return createMultisigSignature();
		}
	}

	//endregion

	//endregion

	//region isFeeValid

	private static abstract class DefaultIsValidCalculation {

		@Test
		public void feeBelowMinimumFeeIsNotValid() {
			// Arrange:
			final Transaction transaction = this.createTransaction();
			// Act:
			final boolean isValid = isRelativeMinimumFeeValid(transaction, -1);

			// Assert:
			Assert.assertThat(isValid, IsEqual.equalTo(false));
		}

		@Test
		public void feeEqualToMinimumFeeIsValid() {
			// Arrange:
			final Transaction transaction = this.createTransaction();
			// Act:
			final boolean isValid = isRelativeMinimumFeeValid(transaction, 0);

			// Assert:
			Assert.assertThat(isValid, IsEqual.equalTo(true));
		}

		@Test
		public void feeAboveMinimumFeeIsValid() {
			// Arrange:
			final Transaction transaction = this.createTransaction();
			// Act:
			final boolean isValid = isRelativeMinimumFeeValid(transaction, 1);

			// Assert:
			Assert.assertThat(isValid, IsEqual.equalTo(true));
		}

		protected abstract Transaction createTransaction();
	}

	public static class TransferIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return createTransfer(100, null);
		}
	}

	public static class MultisigAggregateModificationIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return createMultisigAggregateModification(5);
		}
	}

	public static class ImportanceTransferIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return createImportanceTransfer();
		}
	}

	public static class MultisigIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return createMultisig();
		}
	}

	public static class MultisigSignatureIsValidCalculation {

		@Test
		public void feeBelowMinimumFeeIsNotValid() {
			// Arrange:
			final Transaction transaction = createMultisigSignature();
			// Act:
			final boolean isValid = isRelativeMinimumFeeValid(transaction, -1);

			// Assert:
			Assert.assertThat(isValid, IsEqual.equalTo(false));
		}

		@Test
		public void feeEqualToMinimumFeeIsValid() {
			// Arrange:
			final Transaction transaction = createMultisigSignature();
			// Act:
			final boolean isValid = isRelativeMinimumFeeValid(transaction, 0);

			// Assert:
			Assert.assertThat(isValid, IsEqual.equalTo(true));
		}

		@Test
		public void feeAboveMinimumFeeIsNotValid() {
			// Arrange:
			final Transaction transaction = createMultisigSignature();
			// Act:
			final boolean isValid = isRelativeMinimumFeeValid(transaction, 1);

			// Assert:
			Assert.assertThat(isValid, IsEqual.equalTo(false));
		}
	}

	//endregion

	//region transfer

	//endregion

	//endregion

	//endregion

	//region factories

	private static Transaction createTransfer(final long amount, final Message message) {
		return new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(amount),
				message);
	}

	private static Transaction createMultisigAggregateModification(final int numModifications) {
		final Collection<MultisigCosignatoryModification> modifications = new ArrayList<>();
		for (int i = 0; i < numModifications; ++i) {
			modifications.add(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount()));
		}

		return new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				modifications);
	}

	private static Transaction createImportanceTransfer() {
		return new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				ImportanceTransferMode.Activate,
				Utils.generateRandomAccount());
	}

	private static Transaction createMultisig() {
		return new MultisigTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				createImportanceTransfer());
	}

	private static Transaction createMultisigSignature() {
		return new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				createImportanceTransfer());
	}

	//endregion

	//region other helpers

	private static void assertTransactionFee(final Transaction transaction, final Amount expectedFee) {
		// Act:
		final Amount fee = TransactionFeeCalculator.calculateMinimumFee(transaction, BlockHeight.MAX);

		// Assert:
		Assert.assertThat(fee, IsEqual.equalTo(expectedFee));
	}

	private static boolean isRelativeMinimumFeeValid(final Transaction transaction, final int delta) {
		// Arrange:
		Amount minimumFee = TransactionFeeCalculator.calculateMinimumFee(transaction, BlockHeight.MAX);

		if (delta < 0) {
			minimumFee = minimumFee.subtract(Amount.fromNem(-1 * delta));
		} else if (delta > 0) {
			minimumFee = minimumFee.add(Amount.fromNem(delta));
		}

		transaction.setFee(minimumFee);

		// Act:
		return TransactionFeeCalculator.isFeeValid(transaction, BlockHeight.MAX);
	}

	//endregion
}