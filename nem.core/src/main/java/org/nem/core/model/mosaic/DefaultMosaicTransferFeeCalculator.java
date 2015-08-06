package org.nem.core.model.mosaic;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Quantity;

/**
 * Default implementation for calculating mosaic transfer fees.
 */
public class DefaultMosaicTransferFeeCalculator implements MosaicTransferFeeCalculator {
	private final MosaicTransferFeeInformationLookup mosaicTransferFeeInformationLookup;

	/**
	 * Creates a default mosaic transfer fee calculator.
	 */
	public DefaultMosaicTransferFeeCalculator() {
		this(id -> null);
	}

	/**
	 * Creates a default mosaic transfer fee calculator.
	 *
	 * @param mosaicTransferFeeInformationLookup The mosaic transfer fee information lookup.
	 */
	public DefaultMosaicTransferFeeCalculator(final MosaicTransferFeeInformationLookup mosaicTransferFeeInformationLookup) {
		this.mosaicTransferFeeInformationLookup = mosaicTransferFeeInformationLookup;
	}

	@Override
	public Mosaic calculateFee(final Mosaic mosaic) {
		final MosaicTransferFeeInfo feeInfo = this.mosaicTransferFeeInformationLookup.findById(mosaic.getMosaicId());

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
		final MosaicTransferFeeInfo feeInfo = this.mosaicTransferFeeInformationLookup.findById(mosaic.getMosaicId());
		if (null == feeInfo) {
			throw new IllegalArgumentException(String.format("unable to find fee information for '%s'", mosaic.getMosaicId()));
		}

		return feeInfo.getRecipient();
	}
}
