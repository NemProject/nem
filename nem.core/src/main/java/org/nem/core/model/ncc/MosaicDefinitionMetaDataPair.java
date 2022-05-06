package org.nem.core.model.ncc;

import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.serialization.Deserializer;

/**
 * Pair containing a mosaic definition and meta data.
 */
public class MosaicDefinitionMetaDataPair extends AbstractMetaDataPair<MosaicDefinition, DefaultMetaData> {

	/**
	 * Creates a new pair.
	 *
	 * @param mosaicDefinition The mosaic definition.
	 * @param metaData The meta data.
	 */
	public MosaicDefinitionMetaDataPair(final MosaicDefinition mosaicDefinition, final DefaultMetaData metaData) {
		super("mosaic", "meta", mosaicDefinition, metaData);
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public MosaicDefinitionMetaDataPair(final Deserializer deserializer) {
		super("mosaic", "meta", MosaicDefinition::new, DefaultMetaData::new, deserializer);
	}
}
