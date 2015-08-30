package org.nem.nis.controller.requests;

import org.nem.core.model.mosaic.MosaicId;

/**
 * Builder that is used by Spring to create an MosaicId from a GET request.
 */
public class MosaicIdBuilder {
	private String mosaicId;

	/**
	 * Sets the mosaic id.
	 *
	 * @param mosaicId The mosaic id.
	 */
	public void setMosaicId(final String mosaicId) {
		this.mosaicId = mosaicId;
	}

	/**
	 * Creates a MosaicId.
	 *
	 * @return The mosaic id.
	 */
	public MosaicId build() {
		return new MosaicId(this.mosaicId);
	}
}
