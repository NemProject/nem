package org.nem.nis.mappers;

import org.nem.core.model.Account;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.DbNamespace;

/**
 * A mapping that is able to map a db namespace to a model namespace.
 */
public class NamespaceDbModelToModelMapping implements IMapping<DbNamespace, Namespace> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public NamespaceDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Namespace map(final DbNamespace dbNamespace) {
		final Account owner = this.mapper.map(dbNamespace.getOwner(), Account.class);
		return new Namespace(new NamespaceId(dbNamespace.getFullName()), owner, new BlockHeight(dbNamespace.getHeight()));
	}
}
