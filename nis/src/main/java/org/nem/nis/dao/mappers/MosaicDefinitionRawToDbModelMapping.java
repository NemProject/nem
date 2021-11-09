package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.util.HashSet;

/**
 * A mapping that is able to map raw mosaic definition data to a db mosaic definition.
 */
public class MosaicDefinitionRawToDbModelMapping implements IMapping<Object[], DbMosaicDefinition> {

	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicDefinitionRawToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public DbMosaicDefinition map(final Object[] source) {
		final DbAccount dbCreator = RawMapperUtils.mapAccount(this.mapper, source[1]);
		final DbAccount dbFeeRecipient = RawMapperUtils.mapAccount(this.mapper, source[6]);
		final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
		dbMosaicDefinition.setId(RawMapperUtils.castToLong(source[0]));
		dbMosaicDefinition.setCreator(dbCreator);
		dbMosaicDefinition.setName((String) source[2]);
		dbMosaicDefinition.setDescription((String) source[3]);
		dbMosaicDefinition.setNamespaceId((String) source[4]);
		dbMosaicDefinition.setProperties(new HashSet<>());
		dbMosaicDefinition.setFeeType((Integer) source[5]);
		dbMosaicDefinition.setFeeRecipient(dbFeeRecipient);
		dbMosaicDefinition.setFeeDbMosaicId(RawMapperUtils.castToLong(source[7]));
		dbMosaicDefinition.setFeeQuantity(RawMapperUtils.castToLong(source[8]));
		return dbMosaicDefinition;
	}
}
