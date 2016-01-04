package org.nem.core.model.mosaic;

import org.nem.core.model.primitive.Quantity;

/**
 * Default implementation for calculating mosaic transfer fees.
 */
public class DefaultMosaicTransferFeeCalculator implements MosaicTransferFeeCalculator {
	private final MosaicLevyLookup mosaicLevyLookup;

	/**
	 * Creates a default mosaic transfer fee calculator.
	 */
	public DefaultMosaicTransferFeeCalculator() {
		this(id -> null);
	}

	/**
	 * Creates a default mosaic transfer fee calculator.
	 *
	 * @param mosaicLevyLookup The mosaic levy lookup.
	 */
	public DefaultMosaicTransferFeeCalculator(final MosaicLevyLookup mosaicLevyLookup) {
		this.mosaicLevyLookup = mosaicLevyLookup;
	}

	@Override
	public MosaicLevy calculateAbsoluteLevy(final Mosaic mosaic) {
		final MosaicLevy levy = this.calculateAbsoluteLevyInternal(mosaic);
		return null == levy || Quantity.ZERO.equals(levy.getFee()) ? null : levy;
	}

	private MosaicLevy calculateAbsoluteLevyInternal(final Mosaic mosaic) {
		final MosaicLevy levy = this.mosaicLevyLookup.findById(mosaic.getMosaicId());
		if (null == levy) {
			return null;
		}

		switch (levy.getType()) {
			case Absolute:
				return levy;
			case Percentile:
				// fee is percentile based (10000 is 100%) of the minimum divisible part of the other asset
				return new MosaicLevy(
						MosaicTransferFeeType.Absolute,
						levy.getRecipient(),
						levy.getMosaicId(),
						Quantity.fromValue((mosaic.getQuantity().getRaw() * levy.getFee().getRaw()) / 10_000L));
			default:
				throw new UnsupportedOperationException("cannot calculate fee from unknown fee type");
		}
	}
}
