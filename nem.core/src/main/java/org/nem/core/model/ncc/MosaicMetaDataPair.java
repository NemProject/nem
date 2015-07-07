package org.nem.core.model.ncc;

import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.serialization.*;

/**
 * Pair containing a Mosaic and a DefaultMetaData.
 */
public class MosaicMetaDataPair implements SerializableEntity {
	private final Mosaic mosaic;
	private final DefaultMetaData metaData;

	/**
	 * Creates a new pair.
	 *
	 * @param mosaic The mosaic.
	 * @param metaData The meta data.
	 */
	public MosaicMetaDataPair(final Mosaic mosaic, final DefaultMetaData metaData) {
		this.mosaic = mosaic;
		this.metaData = metaData;
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public MosaicMetaDataPair(final Deserializer deserializer) {
		this.mosaic = deserializer.readObject("mosaic", Mosaic::new);
		this.metaData = deserializer.readObject("meta", DefaultMetaData::new);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("mosaic", this.mosaic);
		serializer.writeObject("meta", this.metaData);
	}

	/**
	 * Gets the mosaic.
	 *
	 * @return The mosaic.
	 */
	public Mosaic getMosaic() {
		return this.mosaic;
	}

	/**
	 * Gets the meta data.
	 *
	 * @return The meta data.
	 */
	public DefaultMetaData getMetaData() {
		return this.metaData;
	}
}
