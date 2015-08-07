package org.nem.core.model.mosaic;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;

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
	public Quantity calculateFee(final Mosaic mosaic) {
		final MosaicTransferFeeInfo feeInfo = this.mosaicTransferFeeInformationLookup.findById(mosaic.getMosaicId());
		if (null == feeInfo || Quantity.ZERO.equals(feeInfo.getFee())) {
			return Quantity.ZERO;
		}

		switch (feeInfo.getType()) {
			case Absolute:
				return feeInfo.getFee();
			case Percentile:
				// TODO 20150806 J-B: i'm not sure why you're dividing by 10_000; isn't this going to be dependent on the supply?
				return Quantity.fromValue((mosaic.getQuantity().getRaw() * feeInfo.getFee().getRaw()) / 10_000L);
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
