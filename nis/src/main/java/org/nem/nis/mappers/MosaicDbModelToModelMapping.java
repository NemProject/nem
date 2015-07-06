package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dbmodel.DbMosaic;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a db mosaic to a model mosaic.
 */
public class MosaicDbModelToModelMapping implements IMapping<DbMosaic, Mosaic> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Mosaic map(final DbMosaic dbMosaic) {
		final Account creator = this.mapper.map(dbMosaic.getCreator(), Account.class);
		final List<NemProperty> properties = dbMosaic.getProperties().stream()
				.map(p -> this.mapper.map(p, NemProperty.class))
				.collect(Collectors.toList());
		return new Mosaic(
				creator,
				new MosaicId(new NamespaceId(dbMosaic.getNamespaceId()), dbMosaic.getName()),
				new MosaicDescriptor(dbMosaic.getDescription()),
				new MosaicPropertiesImpl(properties));
	}
}
