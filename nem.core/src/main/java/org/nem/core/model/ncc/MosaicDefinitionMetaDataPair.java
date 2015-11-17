package org.nem.core.model.ncc;

import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.serialization.Deserializer;

/**
 * Pair containing a mosaic definition and meta data.
 */
public class MosaicDefinitionMetaDataPair extends AbstractMetaDataPair<MosaicDefinition, MosaicMetaData> {

	/**
	 * Creates a new pair.
	 *
	 * @param mosaicDefinition The mosaic definition.
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
