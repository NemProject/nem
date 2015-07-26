package org.nem.nis.mappers;

import org.nem.core.model.mosaic.*;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a model smart tile to a db model smart tile.
 */
public class SmartTileModelToDbModelMapping implements IMapping<MosaicTransferPair, DbSmartTile> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public SmartTileModelToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public DbSmartTile map(final MosaicTransferPair source) {
		final DbMosaicId dbMosaicId = this.mapper.map(source.getMosaicId(), DbMosaicId.class);
		final DbSmartTile dbSmartTile = new DbSmartTile();
		dbSmartTile.setDbMosaicId(dbMosaicId.getId());
		dbSmartTile.setQuantity(source.getQuantity().getRaw());
		return dbSmartTile;
	}
}
