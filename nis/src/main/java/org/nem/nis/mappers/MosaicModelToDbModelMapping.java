package org.nem.nis.mappers;

import org.nem.core.model.mosaic.Mosaic;
import org.nem.nis.dbmodel.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a model mosaic to a db mosaic.
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
	public DbMosaic map(final Mosaic mosaic) {
		final Set<DbMosaicProperty> mosaicProperties = mosaic.getProperties().stream()
				.map(p -> this.mapper.map(p, DbMosaicProperty.class))
				.collect(Collectors.toSet());
		final DbMosaic dbMosaic = new DbMosaic();
		mosaicProperties.forEach(p -> p.setMosaic(dbMosaic));
		dbMosaic.setCreator(this.mapper.map(mosaic.getCreator(), DbAccount.class));
		dbMosaic.setProperties(mosaicProperties);
		dbMosaic.setAmount(mosaic.getAmount().getAmount());
		return dbMosaic;
	}
}
