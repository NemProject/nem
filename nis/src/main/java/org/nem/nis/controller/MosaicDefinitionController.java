package org.nem.nis.controller;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyMosaicDefinitionDao;
import org.nem.nis.dbmodel.DbMosaicDefinition;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

// TODO 20150831 BR -> J: We are having an inconsistent naming for the paths. Our usual approach for naming is:
// > /account/unlock, /transaction/prepare, /chain/hashes-from
// > so we should name it like /mosaic/definition rather than /mosaicDefinition and
// > /account/mosaic/definitions/get rather than /account/mosaic-definitions/get
// > thoughts?
// TODO 20150901 BR -> J: i'm fine with renaming; i think gimre named a few of them, so double-check with him

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

	//region getMosaicDefinition

	/**
	 * Gets the mosaic definition for a given mosaic id.
	 *
	 * @param builder The mosaic id builder.
	 * @return The mosaic definition.
	 */
	@RequestMapping(value = "/mosaic/definition", method = RequestMethod.GET)
	@ClientApi
	public MosaicDefinition getMosaicDefinition(final MosaicIdBuilder builder) {
		final MosaicId mosaicId = builder.build();
		if (MosaicConstants.MOSAIC_ID_XEM.equals(mosaicId)) {
			return MosaicConstants.MOSAIC_DEFINITION_XEM;
		}

		final DbMosaicDefinition dbMosaicDefinition = this.mosaicDefinitionDao.getMosaicDefinition(mosaicId);
		if (null == dbMosaicDefinition) {
			throw new MissingResourceException("invalid mosaic definition", MosaicDefinition.class.getName(), mosaicId.toString());
		}

		return this.map(dbMosaicDefinition);
	}

	//endregion

	//region getMosaicDefinitions

	/**
	 * Gets all known mosaic definitions.
	 *
	 * @param pageBuilder The page builder.
	 * @return All known mosaic definitions.
	 */
	@RequestMapping(value = "/mosaic/definition/page", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicDefinitionMetaDataPair> getMosaicDefinitions(final DefaultPageBuilder pageBuilder) {
		final DefaultPage page = pageBuilder.build();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = this.mosaicDefinitionDao.getMosaicDefinitions(page.getId(), page.getPageSize());
		final Collection<MosaicDefinitionMetaDataPair> pairs = dbMosaicDefinitions.stream()
				.map(md -> new MosaicDefinitionMetaDataPair(this.map(md), new DefaultMetaData(md.getId())))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	//endregion

	//region getNamespaceMosaicDefinitions

	/**
	 * Gets all known mosaic definitions for a namespace.
	 *
	 * @param idBuilder The namespace id builder.
	 * @param pageBuilder The page builder.
	 * @return All known mosaic definitions for the namespace.
	 */
	@RequestMapping(value = "/namespace/mosaic/definition/page", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicDefinitionMetaDataPair> getNamespaceMosaicDefinitions(
			final NamespaceIdBuilder idBuilder,
			final DefaultPageBuilder pageBuilder) {
		final DefaultPage page = pageBuilder.build();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = this.mosaicDefinitionDao.getMosaicDefinitionsForNamespace(
				idBuilder.build(),
				page.getId(),
				page.getPageSize());
		final Collection<MosaicDefinitionMetaDataPair> pairs = dbMosaicDefinitions.stream()
				.map(md -> new MosaicDefinitionMetaDataPair(this.map(md), new DefaultMetaData(md.getId())))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	//endregion

	//region accountMosaicDefinitions

	/**
	 * Gets information about an account's mosaic definitions.
	 *
	 * @param idBuilder The account namespace builder.
	 * @param pageBuilder The page builder.
	 * @return Information about the mosaic definitions owned by an account.
	 */
	@RequestMapping(value = "/account/mosaic/definition/page", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicDefinition> accountMosaicDefinitions(
			final AccountNamespaceBuilder idBuilder,
			final DefaultPageBuilder pageBuilder) {
		final AccountNamespace accountNamespace = idBuilder.build();
		final DefaultPage page = pageBuilder.build();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = this.mosaicDefinitionDao.getMosaicDefinitionsForAccount(
				accountNamespace.getAddress(),
				accountNamespace.getParent(),
				page.getId(),
				page.getPageSize());
		return new SerializableList<>(dbMosaicDefinitions.stream().map(this::map).collect(Collectors.toList()));
	}

	//endregion

	private MosaicDefinition map(final DbMosaicDefinition dbMosaicDefinition) {
		return this.mapper.map(dbMosaicDefinition, MosaicDefinition.class);
	}
}
