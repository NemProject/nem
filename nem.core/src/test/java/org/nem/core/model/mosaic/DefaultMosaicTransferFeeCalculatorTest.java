package org.nem.core.model.mosaic;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

public class DefaultMosaicTransferFeeCalculatorTest {
	private static final Account RECIPIENT = Utils.generateRandomAccount();

	// region calculateAbsoluteLevy

	@Test
	public void levyIsNullForUnknownMosaic() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("foo", 100);

		// Act:
		final MosaicLevy levy = calculator.calculateAbsoluteLevy(mosaic);

		// Assert:
		Assert.assertThat(levy, IsNull.nullValue());
	}

	@Test
	public void feeIsCalculatedCorrectlyWhenFeeTypeIsAbsolute() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("abs3", 123); // fee is 3 * 100

		// Act:
		final MosaicLevy levy = calculator.calculateAbsoluteLevy(mosaic);

		// Assert:
		assertMosaicLevy(levy, RECIPIENT, 13, 3 * 100);
	}

	@Test
	public void levyIsNullIfMosaicHasNoTransferFeeWhenFeeTypeIsAbsolute() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("abs0", 100);

		// Act:
		final MosaicLevy levy = calculator.calculateAbsoluteLevy(mosaic);

		// Assert:
		Assert.assertThat(levy, IsNull.nullValue());
	}

	@Test
	public void feeIsCalculatedCorrectlyWhenFeeTypeIsPercentile() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("per4", 125); // fee is 4 * 100 / 10000 of 125

		// Act:
		final MosaicLevy levy = calculator.calculateAbsoluteLevy(mosaic);

		// Assert:
		assertMosaicLevy(levy, RECIPIENT, 14, 5);
	}

	@Test
	public void levyIsNullIfMosaicHasNoTransferFeeWhenFeeTypeIsPercentile() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("per1", 1); // fee is 1 * 100 / 10000 of 1

		// Act:
		final MosaicLevy levy = calculator.calculateAbsoluteLevy(mosaic);

		// Assert:
		Assert.assertThat(levy, IsNull.nullValue());
	}

	// endregion

	private static Mosaic createMosaic(final String name, final long quantity) {
		final MosaicId mosaicId = new MosaicId(new NamespaceId("foo"), name);
		return new Mosaic(mosaicId, Quantity.fromValue(quantity));
	}

	private static void assertMosaicLevy(final MosaicLevy levy, final Account expectedRecipient, final int expectedMosaicId, final int expectedFee) {
		// Assert:
		Assert.assertThat(levy.getType(), IsEqual.equalTo(MosaicTransferFeeType.Absolute));
		Assert.assertThat(levy.getRecipient(), IsEqual.equalTo(expectedRecipient));
		Assert.assertThat(levy.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(expectedMosaicId)));
		Assert.assertThat(levy.getFee(), IsEqual.equalTo(new Quantity(expectedFee)));
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
