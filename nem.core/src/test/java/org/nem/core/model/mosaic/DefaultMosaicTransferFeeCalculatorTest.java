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
	public void cannotGetFeeForUnknownMosaic() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("foo", 100);

		// Assert:
		ExceptionAssert.assertThrows(v -> calculator.calculateFee(mosaic), IllegalArgumentException.class);
	}

	@Test
	public void returnedMosaicHasQuantityZeroIfFeeInfoHasZeroFee() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("name0", 100);

		// Act:
		final Mosaic feeMosaic = calculator.calculateFee(mosaic);

		// Assert:
		Assert.assertThat(feeMosaic.getMosaicId(), IsEqual.equalTo(new MosaicId(new NamespaceId("foo"), "name0")));
		Assert.assertThat(feeMosaic.getQuantity(), IsEqual.equalTo(Quantity.ZERO));
	}

	@Test
	public void calculateFeeReturnsMosaicWithExpectedQuantityForAbsoluteFeeType() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("name3", 123); // fee is 3 * 100

		// Act:
		final Mosaic feeMosaic = calculator.calculateFee(mosaic);

		// Assert:
		Assert.assertThat(feeMosaic.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(3)));
		Assert.assertThat(feeMosaic.getQuantity(), IsEqual.equalTo(Quantity.fromValue(3 * 100)));
	}

	@Test
	public void calculateFeeReturnsMosaicWithExpectedQuantityForPercentileFeeType() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("name4", 125); // fee is 4 * 100 / 10000 of 125

		// Act:
		final Mosaic feeMosaic = calculator.calculateFee(mosaic);

		// Assert:
		Assert.assertThat(feeMosaic.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(4)));
		Assert.assertThat(feeMosaic.getQuantity(), IsEqual.equalTo(Quantity.fromValue(5)));
	}

	// endregion

	// region getRecipient

	@Test
	public void getRecipientReturnsExpectedRecipient() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = createCalculator();
		final Mosaic mosaic = createMosaic("name1", 100);

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
		final MosaicFeeInformationLookup lookup = id -> {
			if (!id.getName().startsWith("name")) {
				return null;
			}

			final int multiplier = Integer.parseInt(id.getName().substring(4));
			final MosaicTransferFeeType feeType = multiplier % 2 == 1 ? MosaicTransferFeeType.Absolute : MosaicTransferFeeType.Percentile;
			final MosaicId feeMosaicId = multiplier == 0 ? id : Utils.createMosaicId(multiplier);
			final Quantity fee = Quantity.fromValue(100 * multiplier);
			final MosaicTransferFeeInfo feeInfo = new MosaicTransferFeeInfo(feeType, RECIPIENT, feeMosaicId, fee);
			return new MosaicFeeInformation(Supply.ZERO, 0,	feeInfo);
		};

		return new DefaultMosaicTransferFeeCalculator(lookup);
	}
}
