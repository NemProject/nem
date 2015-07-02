package org.nem.nis.cache;

import org.nem.core.model.mosaic.Mosaic;

public interface MosaicCache extends ReadOnlyMosaicCache {

	void add(Mosaic mosaic);

	void remove(Mosaic mosaic);
}
