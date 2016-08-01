package org.nem.core.model;

import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

@RunWith(Enclosed.class)
public class TransactionFeeCalculatorAfterForkTest extends AbstractTransactionFeeCalculatorTest {
	private static final long FEE_UNIT = 2;

	@BeforeClass
	public static void setup() {
		DEFAULT_HEIGHT = new BlockHeight(100);
		final MosaicFeeInformationLookup lookup = AbstractTransactionFeeCalculatorTest.createMosaicFeeInformationLookup();
		NemGlobals.setTransactionFeeCalculator(new DefaultTransactionFeeCalculator(
				lookup,
				() -> DEFAULT_HEIGHT,
				DEFAULT_HEIGHT.prev()));
		setNamespaceAndMosaicRelatedDefaultFee(20);
		setTransactionDefaultFee(6);
		setMultisigSignatureMinimumFee(6);
	}

	@AfterClass
	public static void teardown() {
		Utils.resetGlobals();
	}

	//region calculateMinimumFee

	//region transfer

	public static class TransferMinimumFeeCalculation {
		private static final long MIN_TRANSFER_FEE = 1;

		@Test
		public void feeIsCalculatedCorrectlyForEmptyTransfer() {
			// Assert:
			assertXemFee(0, 0, Amount.fromNem(1));
		}

		@Test
		public void feeIsCalculatedCorrectlyNearTransferStepIncreases() {
			// Assert: fee is initially 1 and increased every 10k xem until is reaches a max fee of 25 xem
			final long step = 10_000;
			for (int i = 0; i < 26; ++i) {
				final long amount = i * step;
				final long fee = Math.max(1, Math.min(25, amount / step));
				assertXemFee(amount, 0, Amount.fromNem(fee));
				assertXemFee(amount + 1, 0, Amount.fromNem(fee));
				assertXemFee(amount + 100, 0, Amount.fromNem(fee));
				assertXemFee(amount + step - 1, 0, Amount.fromNem(fee));
			}
		}

		@Test
		public void feeIsCappedAtTwentyFiveXem() {
			// Assert:
			assertXemFee(250_000, 0, Amount.fromNem(25));
			assertXemFee(250_001, 0, Amount.fromNem(25));
			assertXemFee(500_000, 0, Amount.fromNem(25));
			assertXemFee(1_000_000, 0, Amount.fromNem(25));
			assertXemFee(10_000_000, 0, Amount.fromNem(25));
			assertXemFee(100_000_000, 0, Amount.fromNem(25));
			assertXemFee(1_000_000_000, 0, Amount.fromNem(25));
		}

		@Test
		public void feeIsCalculatedCorrectlyForLargeTransfersWithMessages() {
			// Assert:
			assertXemFee(10000, 96, Amount.fromNem(1 + 4));
			assertXemFee(100000, 128, Amount.fromNem(10 + 5));
			assertXemFee(1000000, 96, Amount.fromNem(25 + 4));
			assertXemFee(2000000, 128, Amount.fromNem(25 + 5));
		}

		@Test
		public void feeIsCalculatedCorrectlyForTransferWithSmallestMessage() {
			// Assert:
			assertXemFee(1200, 1, Amount.fromNem(MIN_TRANSFER_FEE + 1));
		}

		@Test
		public void feeIsCalculatedCorrectlyForTransferNearMessageStepIncreases() {
			// Assert:
			assertXemFee(1200, 31, Amount.fromNem(MIN_TRANSFER_FEE + 1));
			assertXemFee(1200, 32, Amount.fromNem(MIN_TRANSFER_FEE + 2));
			assertXemFee(1200, 33, Amount.fromNem(MIN_TRANSFER_FEE + 2));

			assertXemFee(1200, 63, Amount.fromNem(MIN_TRANSFER_FEE + 2));
			assertXemFee(1200, 64, Amount.fromNem(MIN_TRANSFER_FEE + 3));
			assertXemFee(1200, 65, Amount.fromNem(MIN_TRANSFER_FEE + 3));
		}

		@Test
		public void feeIsCalculatedCorrectlyForTransferWithLargeMessage() {
			// Assert:
			assertXemFee(1200, 96, Amount.fromNem(MIN_TRANSFER_FEE + 4));
			assertXemFee(1200, 128, Amount.fromNem(MIN_TRANSFER_FEE + 5));
			assertXemFee(1200, 256, Amount.fromNem(MIN_TRANSFER_FEE + 9));
			assertXemFee(1200, 320, Amount.fromNem(MIN_TRANSFER_FEE + 11));
		}

		@Test
		public void messageFeeIsBasedOnEncodedSize() {
			// Assert:
			assertMessageFee(96, 128, Amount.fromNem(MIN_TRANSFER_FEE + 4));
			assertMessageFee(128, 96, Amount.fromNem(MIN_TRANSFER_FEE + 5));
		}

		// region mosaic transfers

		// region small business mosaics

		// - A so-called small business mosaic has divisibility of 0 and a max supply of 10000
		// - It is always charged 1 xem fee no matter how many mosaics are transferred
		// - Mosaic 'small business x' has divisibility 0 and supply x * 1000 for x > 0
		// - Mosaic 'small business 0' has divisibility 1 and supply 1000

		@Test
		public void transfersOfManyMosaicsWithDivisibilityZeroAndLowSupplyHaveMinimumFee() {
			// Arrange:
			for (int i=1; i <=10; ++i) {
				// Arrange:
				final TransferTransaction transaction = createTransfer(1, null);
				final MosaicId mosaicId = Utils.createMosaicId("foo", String.format("small business %d", i));
				transaction.getAttachment().addMosaic(mosaicId, Quantity.fromValue(i * 1_000));

				// Assert:
				assertTransactionFee(transaction, Amount.fromNem(1));
			}
		}

		@Test
		public void transfersOfManyMosaicsWithDivisibilityZeroAndSupplyAboveTenThousandDoesNotHaveMinimumFee() {
			// Arrange:
			// - A supply of 11000 means it is not a small business mosaic
			final TransferTransaction transaction = createTransfer(1, null);
			final MosaicId mosaicId = Utils.createMosaicId("foo", "small business 11");
			transaction.getAttachment().addMosaic(mosaicId, Quantity.fromValue(1_000));

			// Assert:
			assertTransactionFee(transaction, Amount.fromNem(4));
		}

		@Test
		public void transfersOfManyMosaicsWithDivisibilityLargerThanZeroDoesNotHaveMinimumFee() {
			// Arrange:
			// - A divisibility of 1 means it is not a small business mosaic
			final TransferTransaction transaction = createTransfer(1, null);
			final MosaicId mosaicId = Utils.createMosaicId("foo", "small business 0");
			transaction.getAttachment().addMosaic(mosaicId, Quantity.fromValue(1_000));

			// Assert:
			assertTransactionFee(transaction, Amount.fromNem(3));
		}

		// endregion

		// mosaic definition data used for the following tests: supply = 100_000_000, divisibility = 3
		// supply ratio: 8_999_999_999 / 100_000_000 ≈ 90
		// 1 / 90 = 0.01111..., so transferring a quantity of 12 is roughly like transferring 1 xem
	}

	//endregion

	//endregion
}
