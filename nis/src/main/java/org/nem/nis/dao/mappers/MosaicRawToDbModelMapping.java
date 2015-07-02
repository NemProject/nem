package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.util.HashSet;

/**
 * A mapping that is able to map raw mosaic data to a db mosaic.
 */
public class MosaicRawToDbModelMapping implements IMapping<Object[], DbMosaic> {

	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicRawToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public DbMosaic map(final Object[] source) {
		final DbAccount dbCreator = RawMapperUtils.mapAccount(this.mapper, source[2]);
		final DbMosaic dbMosaic = new DbMosaic();
		dbMosaic.setId(RawMapperUtils.castToLong(source[1]));
		dbMosaic.setCreator(dbCreator);
		dbMosaic.setAmount(RawMapperUtils.castToLong(source[3]));
		dbMosaic.setPosition((Integer)source[4]);
		dbMosaic.setProperties(new HashSet<>());
		return dbMosaic;
	}
}
