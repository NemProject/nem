package org.nem.core.model.ncc;

import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.serialization.*;

/**
 * Pair containing a mosaic and meta data.
 */
public class MosaicMetaDataPair extends AbstractMetaDataPair<Mosaic, DefaultMetaData> {

	/**
	 * Creates a new pair.
	 *
	 * @param mosaic The mosaic.
	 * @param metaData The meta data.
	 */
	public MosaicMetaDataPair(final Mosaic mosaic, final DefaultMetaData metaData) {
		super("mosaic", "meta", mosaic, metaData);
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public MosaicMetaDataPair(final Deserializer deserializer) {
		super("mosaic", "meta", Mosaic::new, DefaultMetaData::new, deserializer);
	}
}
