package org.nem.core.model.mosaic;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

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

	// region percentile xem fees

	@Test
	public void levyCanBeOneXemForTenMosaicsWhenFeeTypeIsPercentile() {
		// Arrange:
		// - peg a fee of 1 XEM to 10 PEG
		final MosaicLevyLookup lookup = id -> new MosaicLevy(
				MosaicTransferFeeType.Percentile,
				RECIPIENT,
				MosaicConstants.MOSAIC_ID_XEM,
				new Quantity(10000L * Amount.MICRONEMS_IN_NEM / 10L));
		final MosaicTransferFeeCalculator calculator = new DefaultMosaicTransferFeeCalculator(lookup);

		// Act:
		final MosaicLevy levy200 = calculator.calculateAbsoluteLevy(createMosaic("peg", 200));
		final MosaicLevy levy555 = calculator.calculateAbsoluteLevy(createMosaic("peg", 555));
		final MosaicLevy levy800 = calculator.calculateAbsoluteLevy(createMosaic("peg", 800));

		// Assert:
		assertMosaicXemLevy(levy200, Amount.fromNem(20));
		assertMosaicXemLevy(levy555, Amount.fromNem(55).add(Amount.fromMicroNem(500000)));
		assertMosaicXemLevy(levy800, Amount.fromNem(80));
	}

	@Test
	public void levyCanBeTenXemForOneMosaicWhenFeeTypeIsPercentile() {
		// Arrange:
		// - peg a fee of 10 XEM to 1 PEG
		final MosaicLevyLookup lookup = id -> new MosaicLevy(
				MosaicTransferFeeType.Percentile,
				RECIPIENT,
				MosaicConstants.MOSAIC_ID_XEM,
				new Quantity(10000L * Amount.MICRONEMS_IN_NEM * 10));
		final MosaicTransferFeeCalculator calculator = new DefaultMosaicTransferFeeCalculator(lookup);

		// Act:
		final MosaicLevy levy200 = calculator.calculateAbsoluteLevy(createMosaic("peg", 200));
		final MosaicLevy levy555 = calculator.calculateAbsoluteLevy(createMosaic("peg", 555));
		final MosaicLevy levy800 = calculator.calculateAbsoluteLevy(createMosaic("peg", 800));

		// Assert:
		assertMosaicXemLevy(levy200, Amount.fromNem(2000));
		assertMosaicXemLevy(levy555, Amount.fromNem(5550));
		assertMosaicXemLevy(levy800, Amount.fromNem(8000));
	}

	private static void assertMosaicXemLevy(final MosaicLevy levy, final Amount expectedFee) {
		// Assert:
		Assert.assertThat(levy.getType(), IsEqual.equalTo(MosaicTransferFeeType.Absolute));
		Assert.assertThat(levy.getRecipient(), IsEqual.equalTo(RECIPIENT));
		Assert.assertThat(levy.getMosaicId(), IsEqual.equalTo(MosaicConstants.MOSAIC_ID_XEM));
		Assert.assertThat(Amount.fromMicroNem(levy.getFee().getRaw()), IsEqual.equalTo(expectedFee));
	}

	// endregion

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
