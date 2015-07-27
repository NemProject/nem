package org.nem.nis.mappers;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a db model smart tile to a model mosaic.
 */
public class SmartTileDbModelToModelMapping implements IMapping<DbSmartTile, Mosaic> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public SmartTileDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Mosaic map(final DbSmartTile source) {
		final MosaicId mosaicId = this.mapper.map(new DbMosaicId(source.getDbMosaicId()), MosaicId.class);
		return new Mosaic(mosaicId, Quantity.fromValue(source.getQuantity()));
	}
}
