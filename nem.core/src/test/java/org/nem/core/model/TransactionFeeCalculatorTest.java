package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.namespace.*;
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
		private static final long SMALL_TRANSFER_PENALTY = 10;
		private static final long MIN_TRANSFER_FEE = FEE_UNIT;

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
		private static final Boolean MIN_COSIGNATORIES_MODIFICATION_PRESENT = true;

		@Test
		public void feeIsCalculatedCorrectlyForSingleCosignatoryModificationWithoutMinCosignatoriesModification() {
			// Assert:
			assertFee(1, !MIN_COSIGNATORIES_MODIFICATION_PRESENT, Amount.fromNem((5 + 3) * FEE_UNIT));
		}

		@Test
		public void feeIsCalculatedCorrectlyForMultipleCosignatoryModificationsWithoutMinCosignatoriesModification() {
			// Assert:
			for (int i = 2; i < 10; ++i) {
				assertFee(i, !MIN_COSIGNATORIES_MODIFICATION_PRESENT, Amount.fromNem((5 + 3 * i) * FEE_UNIT));
			}
		}

		@Test
		public void feeIsCalculatedCorrectlyForZeroCosignatoryModificationsWithMinCosignatoriesModification() {
			// Assert:
			assertFee(0, MIN_COSIGNATORIES_MODIFICATION_PRESENT, Amount.fromNem((5 + 3) * FEE_UNIT));
		}

		@Test
		public void feeIsCalculatedCorrectlyForSingleCosignatoryModificationWithMinCosignatoriesModification() {
			// Assert:
			assertFee(1, MIN_COSIGNATORIES_MODIFICATION_PRESENT, Amount.fromNem((5 + 3 + 3) * FEE_UNIT));
		}

		@Test
		public void feeIsCalculatedCorrectlyForMultipleCosignatoryModificationsWithMinCosignatoriesModification() {
			// Assert:
			for (int i = 2; i < 10; ++i) {
				assertFee(i, MIN_COSIGNATORIES_MODIFICATION_PRESENT, Amount.fromNem((5 + 3 * i + 3) * FEE_UNIT));
			}
		}

		private static void assertFee(
				final int numModifications,
				final boolean minCosignatoriesModificationPresent,
				final Amount expectedFee) {
			// Arrange:
			final Transaction transaction = createMultisigAggregateModification(numModifications, minCosignatoriesModificationPresent ? 3 : null);

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
			assertTransactionFee(transaction, Amount.fromNem(this.expectedFee()));
		}

		protected abstract Transaction createTransaction();

		protected long expectedFee() {
			return DEFAULT_FEE;
		}
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

	public static class ProvisionNamespaceMinimumFeeCalculation extends DefaultMinimumFeeCalculation {
		protected static final long DEFAULT_FEE = 108;

		@Override
		protected Transaction createTransaction() {
			return createProvisionNamespaceTransaction();
		}

		@Override
		protected long expectedFee() {
			return DEFAULT_FEE;
		}
	}

	public static class MosaicCreationMinimumFeeCalculation extends DefaultMinimumFeeCalculation {

		@Override
		protected Transaction createTransaction() {
			return createMosaicCreationTransaction();
		}
	}

	public static class SmartTileSupplyChangeMinimumFeeCalculation extends DefaultMinimumFeeCalculation {
		protected static final long DEFAULT_FEE = 108;

		@Override
		protected Transaction createTransaction() {
			return createSmartTileSupplyChangeTransaction();
		}

		@Override
		protected long expectedFee() {
			return DEFAULT_FEE;
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

	public static class MultisigAggregateModificationWithoutMinCosignatoriesModificationIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return createMultisigAggregateModification(5, null);
		}
	}

	public static class MultisigAggregateModificationWithMinCosignatoriesModificationIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return createMultisigAggregateModification(5, 3);
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

	public static class ProvisionNamespaceIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return createProvisionNamespaceTransaction();
		}
	}

	public static class MosaicCreationIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return createMosaicCreationTransaction();
		}
	}

	public static class SmartTileSupplyChangeIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return createSmartTileSupplyChangeTransaction();
		}
	}

	public static class MultisigSignatureIsValidCalculation {
		private static final int MINIMUM_FEE = 6;
		private static final int FORK_HEIGHT = 92000;

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
		public void feeAboveMinimumFeeUpToOneThousandXemIsInvalidBeforeForkHeight() {
			// Assert:
			final long[] heights = new long[] { 1, FORK_HEIGHT - 1 };
			assertFeeAboveMinimumFeeUpToOneThousandXemHasExpectedValidityAtHeights(heights, false);
		}

		@Test
		public void feeAboveMinimumFeeUpToOneThousandXemIsValidAtForkHeight() {
			// Assert:
			assertFeeAboveMinimumFeeUpToOneThousandXemHasExpectedValidityAtHeight(FORK_HEIGHT, true);
		}

		@Test
		public void feeAboveMinimumFeeUpToOneThousandXemIsValidAfterForkHeight() {
			// Assert:
			final long[] heights = new long[] { FORK_HEIGHT + 1, FORK_HEIGHT + 10, FORK_HEIGHT + 100 };
			assertFeeAboveMinimumFeeUpToOneThousandXemHasExpectedValidityAtHeights(heights, true);
		}

		public static void assertFeeAboveMinimumFeeUpToOneThousandXemHasExpectedValidityAtHeight(final long height, final boolean expectedResult) {
			// Arrange:
			final Transaction transaction = createMultisigSignature();

			// Assert:
			assertFeeValidationResult(transaction, MINIMUM_FEE + 1, height, expectedResult);
			assertFeeValidationResult(transaction, 10, height, expectedResult);
			assertFeeValidationResult(transaction, 100, height, expectedResult);
			assertFeeValidationResult(transaction, 1000, height, expectedResult);
		}

		public static void assertFeeAboveMinimumFeeUpToOneThousandXemHasExpectedValidityAtHeights(final long[] heights, final boolean expectedResult) {
			// Assert:
			for (final long height : heights) {
				assertFeeAboveMinimumFeeUpToOneThousandXemHasExpectedValidityAtHeight(height, expectedResult);
			}
		}

		@Test
		public void feeAboveOneThousandXemIsInvalid() {
			// Arrange:
			final Transaction transaction = createMultisigSignature();

			// Assert:
			assertFeeValidationResult(transaction, 1001, false);
		}
	}

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

	private static Transaction createMultisigAggregateModification(final int numModifications, final Integer minCosignatories) {
		final Collection<MultisigCosignatoryModification> modifications = new ArrayList<>();
		for (int i = 0; i < numModifications; ++i) {
			modifications.add(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount()));
		}

		return new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				modifications,
				null == minCosignatories ? null : new MultisigMinCosignatoriesModification(minCosignatories));
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

	private static Transaction createProvisionNamespaceTransaction() {
		return new ProvisionNamespaceTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(25000),
				new NamespaceIdPart("bar"),
				new NamespaceId("foo"));
	}

	private static Transaction createMosaicCreationTransaction() {
		return RandomTransactionFactory.createMosaicCreationTransaction(TimeInstant.ZERO, Utils.generateRandomAccount());
	}

	private static Transaction createSmartTileSupplyChangeTransaction() {
		return RandomTransactionFactory.createSmartTileSupplyChangeTransaction(TimeInstant.ZERO, Utils.generateRandomAccount());
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

	private static void assertFeeValidationResult(
			final Transaction transaction,
			final long fee,
			final boolean expectedResult) {
		assertFeeValidationResult(transaction, fee, Long.MAX_VALUE, expectedResult);
	}

	private static void assertFeeValidationResult(
			final Transaction transaction,
			final long fee,
			final long height,
			final boolean expectedResult) {
		// Arrange:
		transaction.setFee(Amount.fromNem(fee));

		// Act:
		final boolean isValid = TransactionFeeCalculator.isFeeValid(transaction, new BlockHeight(height));

		// Assert:
		Assert.assertThat(
				String.format("fee: %d, height: %d", fee, height),
				isValid,
				IsEqual.equalTo(expectedResult));
	}

	//endregion
}