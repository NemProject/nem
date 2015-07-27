package org.nem.nis.mappers;

import org.nem.core.model.mosaic.Mosaic;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a model mosaic to a db model mosaic.
 */
public class MosaicModelToDbModelMapping implements IMapping<Mosaic, DbMosaic> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicModelToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public DbMosaic map(final Mosaic source) {
		final DbMosaicId dbMosaicId = this.mapper.map(source.getMosaicId(), DbMosaicId.class);
		final DbMosaic dbMosaic = new DbMosaic();
		dbMosaic.setDbMosaicId(dbMosaicId.getId());
		dbMosaic.setQuantity(source.getQuantity().getRaw());
		return dbMosaic;
	}
}
