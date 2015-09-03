package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.IntStream;

@RunWith(Enclosed.class)
public class DefaultTransactionFeeCalculatorTest {
	private static final long FEE_UNIT = 2;

	//region calculateMinimumFee

	//region transfer

	public static class TransferMinimumFeeCalculation {
		private static final long SMALL_TRANSFER_PENALTY = 10;
		private static final long MIN_TRANSFER_FEE = FEE_UNIT;

		@Test
		public void feeIsCalculatedCorrectlyForEmptyTransfer() {
			// Assert:
			assertXemFee(0, 0, Amount.fromNem(SMALL_TRANSFER_PENALTY));
		}

		@Test
		public void feeIsCalculatedCorrectlyForPenalizedSmallTransfers() {
			// Assert:
			for (int i = 1; i < 9; ++i) {
				assertXemFee(i, 0, Amount.fromNem(SMALL_TRANSFER_PENALTY - i));
			}
		}

		@Test
		public void feeIsCalculatedCorrectlyForNonPenalizedSmallTransfers() {
			// Assert:
			for (int i = 9; i < 20; ++i) {
				assertXemFee(i, 0, Amount.fromNem(MIN_TRANSFER_FEE));
			}
		}

		@Test
		public void feeIsCalculatedCorrectlyNearTransferStepIncreases() {
			// Assert:
			assertXemFee(4546, 0, Amount.fromNem(2));
			assertXemFee(4547, 0, Amount.fromNem(3));
			assertXemFee(4548, 0, Amount.fromNem(3));

			assertXemFee(6063, 0, Amount.fromNem(3));
			assertXemFee(6064, 0, Amount.fromNem(4));
			assertXemFee(6065, 0, Amount.fromNem(4));
		}

		@Test
		public void feeIsCalculatedCorrectlyForLargeTransfers() {
			// Assert:
			assertXemFee(10000, 0, Amount.fromNem(6));
			assertXemFee(100000, 0, Amount.fromNem(58));
			assertXemFee(1000000, 0, Amount.fromNem(140));
			assertXemFee(2000000, 0, Amount.fromNem(148));
		}

		@Test
		public void feeIsCalculatedCorrectlyForLargeTransfersWithMessages() {
			// Assert:
			assertXemFee(10000, 96, Amount.fromNem(6 + 12));
			assertXemFee(100000, 128, Amount.fromNem(58 + 16));
			assertXemFee(1000000, 96, Amount.fromNem(140 + 12));
			assertXemFee(2000000, 128, Amount.fromNem(148 + 16));
		}

		@Test
		public void feeIsCalculatedCorrectlyForTransferWithSmallestMessage() {
			// Assert:
			assertXemFee(1200, 1, Amount.fromNem(MIN_TRANSFER_FEE + 2));
		}

		@Test
		public void feeIsCalculatedCorrectlyForTransferNearMessageStepIncreases() {
			// Assert:
			assertXemFee(1200, 31, Amount.fromNem(MIN_TRANSFER_FEE + 2));
			assertXemFee(1200, 32, Amount.fromNem(MIN_TRANSFER_FEE + 4));
			assertXemFee(1200, 33, Amount.fromNem(MIN_TRANSFER_FEE + 4));

			assertXemFee(1200, 63, Amount.fromNem(MIN_TRANSFER_FEE + 6));
			assertXemFee(1200, 64, Amount.fromNem(MIN_TRANSFER_FEE + 8));
			assertXemFee(1200, 65, Amount.fromNem(MIN_TRANSFER_FEE + 8));
		}

		@Test
		public void feeIsCalculatedCorrectlyForTransferWithLargeMessage() {
			// Assert:
			assertXemFee(1200, 96, Amount.fromNem(MIN_TRANSFER_FEE + 12));
			assertXemFee(1200, 128, Amount.fromNem(MIN_TRANSFER_FEE + 16));
			assertXemFee(1200, 256, Amount.fromNem(MIN_TRANSFER_FEE + 32));
		}

		@Test
		public void messageFeeIsBasedOnEncodedSize() {
			// Assert:
			assertMessageFee(96, 128, Amount.fromNem(SMALL_TRANSFER_PENALTY + 12));
			assertMessageFee(128, 96, Amount.fromNem(SMALL_TRANSFER_PENALTY + 16));
		}

		// region mosaic transfers

		// mosaic definition data used for the following tests: supply = 100_000_000, divisibility = 3
		// supply ratio: 8_999_999_999 / 100_000_000 ≈ 90
		// 1 / 90 = 0.01111..., so transferring a quantity of 12 is roughly like transferring 1 xem
		// In comparison to a xem transfer, equivalent mosaic transfers have 25% higher fees (rounded)

		// note that xem as a mosaic transfer is also 25% higher than a regular xem transfer because
		// it takes up more space in the db; this is validated by
		// feesForMosaicTransfersAreTwentyFivePercentHigherThanEquivalentXemTransfers

		@Test
		public void feeIsCalculatedCorrectlyForPenalizedSmallMosaicTransfers() {
			// Assert:
			for (int i = 1; i < 9; ++i) {
				assertSingleMosaicFee(1, 0, i * 12L, Amount.fromNem((SMALL_TRANSFER_PENALTY - i) * 5 / 4));
			}
		}

		@Test
		public void feeIsCalculatedCorrectlyForNonPenalizedSmallMosaicTransfers() {
			// Assert:
			for (int i = 9; i < 20; ++i) {
				assertSingleMosaicFee(1, 0, i * 12L, Amount.fromNem(MIN_TRANSFER_FEE * 5 / 4));
			}
		}

		@Test
		public void feeIsCalculatedCorrectlyNearMosaicTransferStepIncreases() {
			// Assert:
			// 2 -> 3 roughly at 50.5 units
			assertSingleMosaicFee(1, 0, 50_000L, Amount.fromNem(2 * 5 / 4));
			assertSingleMosaicFee(1, 0, 51_000L, Amount.fromNem(3 * 5 / 4));
			assertSingleMosaicFee(1, 0, 52_000L, Amount.fromNem(3 * 5 / 4));

			// 3 -> 5 roughly at 67.3 units
			assertSingleMosaicFee(1, 0, 67_000L, Amount.fromNem(3 * 5 / 4));
			assertSingleMosaicFee(1, 0, 68_000L, Amount.fromNem(4 * 5 / 4));
			assertSingleMosaicFee(1, 0, 69_000L, Amount.fromNem(4 * 5 / 4));
		}

		@Test
		public void feeIsCalculatedCorrectlyForLargeMosaicTransfers() {
			// Assert:
			assertSingleMosaicFee(1, 0, 111_000L, Amount.fromNem(6 * 5 / 4));
			assertSingleMosaicFee(1, 0, 1_110_000L, Amount.fromNem(58 * 5 / 4));
			assertSingleMosaicFee(1, 0, 11_100_000L, Amount.fromNem(140 * 5 / 4));
			assertSingleMosaicFee(1, 0, 22_200_000L, Amount.fromNem(148 * 5 / 4));
		}

		@Test
		public void feeIsCalculatedCorrectlyForMosaicTransfersWithAmountOtherThanOne() {
			// Assert:
			assertSingleMosaicFee(1, 0, 1_000_000L, Amount.fromNem(53 * 5 / 4));
			assertSingleMosaicFee(2, 0, 500_000L, Amount.fromNem(53 * 5 / 4));
			assertSingleMosaicFee(5, 0, 200_000L, Amount.fromNem(53 * 5 / 4));
			assertSingleMosaicFee(10, 0, 100_000L, Amount.fromNem(53 * 5 / 4));
			assertSingleMosaicFee(500, 0, 2_000L, Amount.fromNem(53 * 5 / 4));
			assertSingleMosaicFee(10_000, 0, 100L, Amount.fromNem(53 * 5 / 4));
			assertSingleMosaicFee(1_000_000, 0, 1L, Amount.fromNem(53 * 5 / 4));
		}

		@Test
		public void messageFeeIsAddedToMosaicTransferFee() {
			// Assert:
			assertSingleMosaicFee(1, 31, 1_000_000L, Amount.fromNem(53 * 5 / 4 + 2));
			assertSingleMosaicFee(1, 47, 1_000_000L, Amount.fromNem(53 * 5 / 4 + 4));
			assertSingleMosaicFee(1, 63, 1_000_000L, Amount.fromNem(53 * 5 / 4 + 6));
			assertSingleMosaicFee(1, 79, 1_000_000L, Amount.fromNem(53 * 5 / 4 + 8));
		}

		@Test
		public void feesAreAddedWhenTransferringSeveralMosaics() {
			// Arrange: mosaic definitions are (100M, 3), (200M, 4), (300M, 5)
			final Transaction transaction = createTransferWithMosaics(1, 0, 111_000L, 11_100_000L, 1_110_000_000L);

			// Assert:
			assertTransactionFee(transaction, Amount.fromNem((6 + 31 + 113) * 5 / 4));
		}

		@Test
		public void feesForMosaicTransfersAreTwentyFivePercentHigherThanEquivalentXemTransfers() {
			// Assert:
			IntStream.range(0, 100).forEach(i -> assertXemTransferToMosaicTransferFeeRatio(1000 * i));
		}

		@Test
		public void feesForMosaicTransfersAreTenIfMosaicSupplyIsZero() {
			// Arrange:
			final TransferTransaction transaction = createTransfer(5, null);
			final MosaicId mosaicId = Utils.createMosaicId("foo", "zero supply");
			transaction.getAttachment().addMosaic(mosaicId, Quantity.fromValue(1_000_000));

			// Assert:
			// - zero supply means zero xem equivalent and therefore a penalty fee of 10 * 1.25 xem = 12 xem
			assertTransactionFee(transaction, Amount.fromNem(12));
		}

		@Test
		public void feesCannotBeCalculatedForUnknownMosaic() {
			// Arrange:
			final TransferTransaction transaction = createTransfer(1L, null);
			transaction.getAttachment().addMosaic(Utils.createMosaic("foo", "tokens"));
			final TransactionFeeCalculator calculator = createCalculator();

			// Act:
			ExceptionAssert.assertThrows(
					v -> calculator.calculateMinimumFee(transaction),
					IllegalArgumentException.class);
		}

		// endregion

		private static void assertXemFee(final long amount, final int messageSize, final Amount expectedFee) {
			// Arrange:
			final Message message = 0 == messageSize ? null : new PlainMessage(new byte[messageSize]);
			final Transaction transaction = createTransfer(amount, message);

			// Assert:
			assertTransactionFee(transaction, expectedFee);
		}

		private static void assertSingleMosaicFee(final long amount, final int messageSize, final long quantity, final Amount expectedFee) {
			// Arrange:
			final Transaction transaction = createTransferWithMosaics(amount, messageSize, quantity);

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

		private static Transaction createTransferWithMosaics(final long amount, final int messageSize, final long... quantities) {
			final Message message = 0 == messageSize ? null : new PlainMessage(new byte[messageSize]);
			final TransferTransaction transaction = createTransfer(amount, message);
			IntStream.range(0, quantities.length)
					.forEach(i -> transaction.getAttachment().addMosaic(Utils.createMosaicId(i + 1), Quantity.fromValue(quantities[i])));
			return transaction;
		}

		private static void assertXemTransferToMosaicTransferFeeRatio(final long amount) {
			// Arrange:
			final TransactionFeeCalculator calculator = createCalculator();
			final TransferTransaction xemTransfer = createTransfer(amount, null);
			final long xemFee = calculator.calculateMinimumFee(xemTransfer).getNumNem();
			final TransferTransaction mosaicTransfer = createTransfer(1, null);
			mosaicTransfer.getAttachment().addMosaic(MosaicConstants.MOSAIC_ID_XEM, Quantity.fromValue(amount * Amount.MICRONEMS_IN_NEM));
			final long mosaicFee = calculator.calculateMinimumFee(mosaicTransfer).getNumNem();

			// Assert:
			Assert.assertThat(mosaicFee, IsEqual.equalTo((xemFee * 5) / 4));
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
			return RandomTransactionFactory.createImportanceTransfer();
		}
	}

	public static class MultisigMinimumFeeCalculation extends DefaultMinimumFeeCalculation {

		@Override
		protected Transaction createTransaction() {
			return RandomTransactionFactory.createMultisigTransfer();
		}
	}

	public static class MultisigSignatureMinimumFeeCalculation extends DefaultMinimumFeeCalculation {

		@Override
		protected Transaction createTransaction() {
			return RandomTransactionFactory.createMultisigSignature();
		}
	}

	public static class ProvisionNamespaceMinimumFeeCalculation extends DefaultMinimumFeeCalculation {
		protected static final long DEFAULT_FEE = 108;

		@Override
		protected Transaction createTransaction() {
			return RandomTransactionFactory.createProvisionNamespaceTransaction();
		}

		@Override
		protected long expectedFee() {
			return DEFAULT_FEE;
		}
	}

	public static class MosaicDefinitionCreationMinimumFeeCalculation extends DefaultMinimumFeeCalculation {
		protected static final long DEFAULT_FEE = 108;

		@Override
		protected Transaction createTransaction() {
			return RandomTransactionFactory.createMosaicDefinitionCreationTransaction();
		}

		@Override
		protected long expectedFee() {
			return DEFAULT_FEE;
		}
	}

	public static class MosaicSupplyChangeMinimumFeeCalculation extends DefaultMinimumFeeCalculation {
		protected static final long DEFAULT_FEE = 108;

		@Override
		protected Transaction createTransaction() {
			return RandomTransactionFactory.createMosaicSupplyChangeTransaction();
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
			return RandomTransactionFactory.createImportanceTransfer();
		}
	}

	public static class MultisigIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return RandomTransactionFactory.createMultisigTransfer();
		}
	}

	public static class ProvisionNamespaceIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return RandomTransactionFactory.createProvisionNamespaceTransaction();
		}
	}

	public static class MosaicDefinitionCreationIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return RandomTransactionFactory.createMosaicDefinitionCreationTransaction();
		}
	}

	public static class MosaicSupplyChangeIsValidCalculation extends DefaultIsValidCalculation {

		@Override
		protected Transaction createTransaction() {
			return RandomTransactionFactory.createMosaicSupplyChangeTransaction();
		}
	}

	public static class MultisigSignatureIsValidCalculation {
		private static final int MINIMUM_FEE = 6;
		private static final int FORK_HEIGHT = 92000;

		@Test
		public void feeBelowMinimumFeeIsNotValid() {
			// Arrange:
			final Transaction transaction = RandomTransactionFactory.createMultisigSignature();
			// Act:
			final boolean isValid = isRelativeMinimumFeeValid(transaction, -1);

			// Assert:
			Assert.assertThat(isValid, IsEqual.equalTo(false));
		}

		@Test
		public void feeEqualToMinimumFeeIsValid() {
			// Arrange:
			final Transaction transaction = RandomTransactionFactory.createMultisigSignature();

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
			final Transaction transaction = RandomTransactionFactory.createMultisigSignature();

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
			final Transaction transaction = RandomTransactionFactory.createMultisigSignature();

			// Assert:
			assertFeeValidationResult(transaction, 1001, false);
		}
	}

	//endregion

	//region factories

	private static TransferTransaction createTransfer(final long amount, final Message message) {
		return new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(amount),
				new TransferTransactionAttachment(message));
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

	//endregion

	//region other helpers

	private static void assertTransactionFee(final Transaction transaction, final Amount expectedFee) {
		// Act:
		final Amount fee = createCalculator().calculateMinimumFee(transaction);

		// Assert:
		Assert.assertThat(fee, IsEqual.equalTo(expectedFee));
	}

	private static boolean isRelativeMinimumFeeValid(final Transaction transaction, final int delta) {
		// Arrange:
		Amount minimumFee = createCalculator().calculateMinimumFee(transaction);

		if (delta < 0) {
			minimumFee = minimumFee.subtract(Amount.fromNem(-1 * delta));
		} else if (delta > 0) {
			minimumFee = minimumFee.add(Amount.fromNem(delta));
		}

		transaction.setFee(minimumFee);

		// Act:
		return createCalculator().isFeeValid(transaction, BlockHeight.MAX);
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
		final boolean isValid = createCalculator().isFeeValid(transaction, new BlockHeight(height));

		// Assert:
		Assert.assertThat(
				String.format("fee: %d, height: %d", fee, height),
				isValid,
				IsEqual.equalTo(expectedResult));
	}

	private static TransactionFeeCalculator createCalculator() {
		final MosaicFeeInformationLookup lookup = id -> {
			if (id.getName().equals("xem")) {
				return new MosaicFeeInformation(Supply.fromValue(8_999_999_999L), 6);
			}

			if (id.getName().equals("zero supply")) {
				return new MosaicFeeInformation(Supply.ZERO, 3);
			}

			if (!id.getName().startsWith("name")) {
				return null;
			}

			final int multiplier = Integer.parseInt(id.getName().substring(4));
			final int divisibilityChange = multiplier - 1;
			return new MosaicFeeInformation(
					Supply.fromValue(100_000_000 * multiplier),
					3 + divisibilityChange);
		};

		return new DefaultTransactionFeeCalculator(lookup);
	}

	//endregion
}