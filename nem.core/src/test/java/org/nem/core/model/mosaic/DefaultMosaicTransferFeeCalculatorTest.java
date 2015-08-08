package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

public class DefaultMosaicTransferFeeCalculatorTest {
	private static final Account RECIPIENT = Utils.generateRandomAccount();

	// region calculateFee

	@Test
	public void feeIsZeroForUnknownMosaic() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("foo", 100);

		// Act:
		final Quantity fee = calculator.calculateFee(mosaic);

		// Assert:
		Assert.assertThat(fee, IsEqual.equalTo(Quantity.ZERO));
	}

	@Test
	public void feeIsZeroIfMosaicHasNoTransferFee() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("abs0", 100);

		// Act:
		final Quantity fee = calculator.calculateFee(mosaic);

		// Assert:
		Assert.assertThat(fee, IsEqual.equalTo(Quantity.ZERO));
	}

	@Test
	public void feeIsCalculatedCorrectlyWhenFeeTypeIsAbsolute() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("abs3", 123); // fee is 3 * 100

		// Act:
		final Quantity fee = calculator.calculateFee(mosaic);

		// Assert:
		Assert.assertThat(fee, IsEqual.equalTo(Quantity.fromValue(3 * 100)));
	}

	@Test
	public void feeIsCalculatedCorrectlyWhenFeeTypeIsPercentile() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("per4", 125); // fee is 4 * 100 / 10000 of 125

		// Act:
		final Quantity fee = calculator.calculateFee(mosaic);

		// Assert:
		Assert.assertThat(fee, IsEqual.equalTo(Quantity.fromValue(5)));
	}

	// endregion

	// region getRecipient

	@Test
	public void getRecipientReturnsExpectedRecipient() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("abs1", 100);

		// Act:
		final Account recipient = calculator.getFeeRecipient(mosaic);

		// Assert:
		Assert.assertThat(recipient, IsEqual.equalTo(RECIPIENT));
	}

	@Test
	public void cannotGetRecipientForUnknownMosaic() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("foo", 100);

		// Assert:
		ExceptionAssert.assertThrows(v -> calculator.getFeeRecipient(mosaic), IllegalArgumentException.class);
	}

	// endregion

	private static Mosaic createMosaic(final String name, final long quantity) {
		final MosaicId mosaicId = new MosaicId(new NamespaceId("foo"), name);
		return new Mosaic(mosaicId, Quantity.fromValue(quantity));
	}

	private static MosaicTransferFeeCalculator createCalculator() {
		final MosaicLevyLookup lookup = id -> {
			final MosaicTransferFeeType feeType;
			if (id.getName().startsWith("abs")) {
				feeType = MosaicTransferFeeType.Absolute;
			} else if (id.getName().startsWith("per")) {
				feeType = MosaicTransferFeeType.Percentile;
			} else {
				return null;
			}

			final int multiplier = Integer.parseInt(id.getName().substring(3));
			final Quantity fee = Quantity.fromValue(100 * multiplier);
			return new MosaicLevy(feeType, RECIPIENT, Utils.createMosaicId(multiplier + 10), fee);
		};

		return new DefaultMosaicTransferFeeCalculator(lookup);
	}
}
