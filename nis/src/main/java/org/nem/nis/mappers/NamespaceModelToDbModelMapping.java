package org.nem.nis.mappers;

import org.nem.core.model.namespace.Namespace;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a model namespace to a db namespace.
 */
public class NamespaceModelToDbModelMapping implements IMapping<Namespace, DbNamespace> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public NamespaceModelToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public DbNamespace map(final Namespace namespace) {
		final DbNamespace dbNamespace = new DbNamespace();
		dbNamespace.setFullName(namespace.getId().toString());
		dbNamespace.setOwner(this.mapper.map(namespace.getOwner(), DbAccount.class));
		dbNamespace.setHeight(namespace.getHeight().getRaw());
		dbNamespace.setLevel(namespace.getId().getLevel());
		return dbNamespace;
	}
}
