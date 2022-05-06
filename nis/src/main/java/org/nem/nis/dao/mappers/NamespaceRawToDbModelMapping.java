package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

/**
 * A mapping that is able to map raw namespace data to a db namespace.
 */
public class NamespaceRawToDbModelMapping implements IMapping<Object[], DbNamespace> {

	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public NamespaceRawToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public DbNamespace map(final Object[] source) {
		final DbAccount dbOwner = RawMapperUtils.mapAccount(this.mapper, source[2]);
		final DbNamespace dbNamespace = new DbNamespace();
		dbNamespace.setId(RawMapperUtils.castToLong(source[0]));
		dbNamespace.setFullName((String) source[1]);
		dbNamespace.setOwner(dbOwner);
		dbNamespace.setHeight(RawMapperUtils.castToLong(source[3]));
		dbNamespace.setLevel((Integer) source[4]);
		return dbNamespace;
	}
}
