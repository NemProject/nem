package org.nem.core.model;

import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.model.mosaic.MosaicFeeInformationLookup;
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

	}

	//endregion

	//endregion
}
