package org.nem.nis.controller;

import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyMosaicDefinitionDao;
import org.nem.nis.dbmodel.DbMosaicDefinition;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * REST mosaic definition controller.
 */
@RestController
public class MosaicDefinitionController {
	private final ReadOnlyMosaicDefinitionDao mosaicDefinitionDao;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	MosaicDefinitionController(
			final ReadOnlyMosaicDefinitionDao mosaicDefinitionDao,
			final NisDbModelToModelMapper mapper) {
		this.mosaicDefinitionDao = mosaicDefinitionDao;
		this.mapper = mapper;
	}

	//region getMosaicDefinitions

	/**
	 * Gets all known mosaic definitions.
	 *
	 * @param pageBuilder The page builder.
	 * @return All known mosaic definitions.
	 */
	@RequestMapping(value = "/mosaicDefinitions", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicDefinitionMetaDataPair> getMosaicDefinitions(final DefaultPageBuilder pageBuilder) {
		final DefaultPage page = pageBuilder.build();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = this.mosaicDefinitionDao.getMosaicDefinitions(page.getId(), page.getPageSize());
		final Collection<MosaicDefinitionMetaDataPair> pairs = dbMosaicDefinitions.stream()
				.map(n -> new MosaicDefinitionMetaDataPair(this.mapper.map(n), new DefaultMetaData(n.getId())))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	//endregion

	//region getNamespaceMosaicDefinitions

	/**
	 * Gets all known mosaic definitions for a namespace.
	 *
	 * @param pageBuilder The page builder.
	 * @return All known mosaic definitions for the namespace.
	 */
	@RequestMapping(value = "/namespace/mosaicDefinitions", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicDefinitionMetaDataPair> getNamespaceMosaicDefinitions(final NamespaceIdMaxIdPageBuilder pageBuilder) {
		final NamespaceIdMaxIdPage page = pageBuilder.build();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = this.mosaicDefinitionDao.getMosaicDefinitionsForNamespace(
				page.getNamespaceId(),
				page.getId(),
				page.getPageSize());
		final Collection<MosaicDefinitionMetaDataPair> pairs = dbMosaicDefinitions.stream()
				.map(n -> new MosaicDefinitionMetaDataPair(
						this.mapper.map(n),
						new DefaultMetaData(n.getId())))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	//endregion
}
