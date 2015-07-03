package org.nem.nis.cache;

import org.nem.core.model.mosaic.Mosaic;

/**
 * General class for holding mosaics.
 * TODO 20150702 J-*: placeholder
 */
public class DefaultMosaicCache implements MosaicCache, CopyableCache<DefaultMosaicCache> {

	@Override
	public void add(final Mosaic mosaic) {

	}

	@Override
	public void remove(final Mosaic mosaic) {

	}

	@Override
	public void shallowCopyTo(final DefaultMosaicCache rhs) {
//		throw new UnsupportedOperationException("not implemented yet");
	}

	@Override
	public DefaultMosaicCache copy() {
		return new DefaultMosaicCache();
	}
}
