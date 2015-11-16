package org.nem.core.model.mosaic;

import org.nem.core.model.ncc.AbstractMetaDataPair;
import org.nem.core.serialization.Deserializer;

public class MosaicDefinitionMetaDataPair  extends AbstractMetaDataPair<MosaicDefinition, MosaicMetaData> {
	/**
	 * Creates a new pair.
	 *
	 * @param mosaicDefinition The mosaicDefinition.
	 * @param metaData The meta data.
	 */
	public MosaicDefinitionMetaDataPair(final MosaicDefinition mosaicDefinition, final MosaicMetaData metaData) {
		super("mosaicDefinition", "meta", mosaicDefinition, metaData);
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public MosaicDefinitionMetaDataPair(final Deserializer deserializer) {
		super("mosaicDefinition", "meta", MosaicDefinition::new, MosaicMetaData::new, deserializer);
	}
}
