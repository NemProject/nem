package org.nem.core.model.mosaic;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Quantity;

/**
 * Default implementation for calculating mosaic transfer fees.
 */
public class DefaultMosaicTransferFeeCalculator implements MosaicTransferFeeCalculator {
	private final MosaicFeeInformationLookup mosaicFeeInformationLookup;

	/**
	 * Creates a default mosaic transfer fee calculator.
	 */
	public DefaultMosaicTransferFeeCalculator() {
		this(id -> null);
	}

	/**
	 * Creates a default mosaic transfer fee calculator.
	 */
	public DefaultMosaicTransferFeeCalculator(final MosaicFeeInformationLookup mosaicFeeInformationLookup) {
		this.mosaicFeeInformationLookup = mosaicFeeInformationLookup;
	}

	@Override
	public Mosaic calculateFee(final Mosaic mosaic) {
		final MosaicFeeInformation information = this.mosaicFeeInformationLookup.findById(mosaic.getMosaicId());
		if (null == information) {
			throw new IllegalArgumentException(String.format("unable to find fee information for '%s'", mosaic.getMosaicId()));
		}

		final MosaicTransferFeeInfo feeInfo = information.getTransferFeeInfo();
		if (Quantity.ZERO.equals(feeInfo.getFee())) {
			return new Mosaic(feeInfo.getMosaicId(), Quantity.ZERO);
		}

		switch (feeInfo.getType()) {
			case Absolute:
				return new Mosaic(feeInfo.getMosaicId(), feeInfo.getFee());
			case Percentile:
				final Quantity quantity = Quantity.fromValue((mosaic.getQuantity().getRaw() * feeInfo.getFee().getRaw()) / 10_000L);
				return new Mosaic(feeInfo.getMosaicId(), quantity);
			default:
				throw new UnsupportedOperationException("cannot calculate fee from unknown fee type");
		}
	}

	@Override
	public Account getFeeRecipient(final Mosaic mosaic) {
		final MosaicFeeInformation information = this.mosaicFeeInformationLookup.findById(mosaic.getMosaicId());
		if (null == information) {
			throw new IllegalArgumentException(String.format("unable to find fee information for '%s'", mosaic.getMosaicId()));
		}

		return information.getTransferFeeInfo().getRecipient();
	}
}
