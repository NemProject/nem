package org.nem.nis.controller;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyMosaicDefinitionDao;
import org.nem.nis.dbmodel.DbMosaicDefinition;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.MosaicInfoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * REST mosaic definition controller.
 */
@RestController
public class MosaicDefinitionController {
	private final ReadOnlyMosaicDefinitionDao mosaicDefinitionDao;
	private final NisDbModelToModelMapper mapper;
	private final MosaicInfoFactory mosaicInfoFactory;

	@Autowired(required = true)
	MosaicDefinitionController(final ReadOnlyMosaicDefinitionDao mosaicDefinitionDao, final NisDbModelToModelMapper mapper,
			final MosaicInfoFactory mosaicInfoFactory) {
		this.mosaicDefinitionDao = mosaicDefinitionDao;
		this.mapper = mapper;
		this.mosaicInfoFactory = mosaicInfoFactory;
	}

	// region getMosaicDefinition

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

		// check if parent namespace exists
		if (!this.mosaicInfoFactory.isNamespaceActive(mosaicId.getNamespaceId())) {
			throw new MissingResourceException("invalid mosaic definition", MosaicDefinition.class.getName(), mosaicId.toString());
		}

		final DbMosaicDefinition dbMosaicDefinition = this.mosaicDefinitionDao.getMosaicDefinition(mosaicId);
		if (null == dbMosaicDefinition) {
			throw new MissingResourceException("invalid mosaic definition", MosaicDefinition.class.getName(), mosaicId.toString());
		}

		return this.map(dbMosaicDefinition);
	}

	// endregion

	// region getMosaicDefinitions

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
		final Collection<MosaicDefinitionMetaDataPair> pairs = recursivelyGetMosaicDefinitions(
				this.mosaicDefinitionDao::getMosaicDefinitions, page);
		return new SerializableList<>(pairs);
	}

	// endregion

	// region getNamespaceMosaicDefinitions

	/**
	 * Gets all known mosaic definitions for a namespace.
	 *
	 * @param idBuilder The namespace id builder.
	 * @param pageBuilder The page builder.
	 * @return All known mosaic definitions for the namespace.
	 */
	@RequestMapping(value = "/namespace/mosaic/definition/page", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicDefinitionMetaDataPair> getNamespaceMosaicDefinitions(final NamespaceIdBuilder idBuilder,
			final DefaultPageBuilder pageBuilder) {
		final NamespaceId namespaceId = idBuilder.build();
		if (!this.mosaicInfoFactory.isNamespaceActive(namespaceId)) {
			return new SerializableList<>(0);
		}

		final DefaultPage page = pageBuilder.build();
		final Collection<DbMosaicDefinition> dbMosaicDefinitions = this.mosaicDefinitionDao
				.getMosaicDefinitionsForNamespace(idBuilder.build(), page.getId(), page.getPageSize());
		final Collection<MosaicDefinitionMetaDataPair> pairs = dbMosaicDefinitions.stream()
				.map(md -> new MosaicDefinitionMetaDataPair(this.map(md), new DefaultMetaData(md.getId()))).collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	// endregion

	// region accountMosaicDefinitions

	/**
	 * Gets information about an account's mosaic definitions.
	 *
	 * @param idBuilder The account namespace builder.
	 * @param pageBuilder The page builder.
	 * @return Information about the mosaic definitions owned by an account.
	 */
	@RequestMapping(value = "/account/mosaic/definition/page", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicDefinition> accountMosaicDefinitions(final AccountNamespaceBuilder idBuilder,
			final DefaultPageBuilder pageBuilder) {
		final AccountNamespace accountNamespace = idBuilder.build();
		final DefaultPage page = pageBuilder.build();
		final BiFunction<Long, Integer, Collection<DbMosaicDefinition>> retriever = (id, pageSize) -> {
			return this.mosaicDefinitionDao.getMosaicDefinitionsForAccount(accountNamespace.getAddress(), accountNamespace.getParent(), id,
					pageSize);
		};
		final Collection<MosaicDefinitionMetaDataPair> pairs = recursivelyGetMosaicDefinitions(retriever, page);
		return new SerializableList<>(pairs.stream().map(MosaicDefinitionMetaDataPair::getEntity).collect(Collectors.toList()));
	}

	// endregion

	private MosaicDefinition map(final DbMosaicDefinition dbMosaicDefinition) {
		return this.mapper.map(dbMosaicDefinition, MosaicDefinition.class);
	}

	private Collection<MosaicDefinitionMetaDataPair> recursivelyGetMosaicDefinitions(
			final BiFunction<Long, Integer, Collection<DbMosaicDefinition>> retriever, final DefaultPage page) {
		Long[] curDbId = {
				page.getId()
		};
		final Collection<MosaicDefinitionMetaDataPair> pairs = new ArrayList<>();
		while (pairs.size() < page.getPageSize()) {
			final Collection<DbMosaicDefinition> dbMosaicDefinitions = retriever.apply(curDbId[0], page.getPageSize());
			if (dbMosaicDefinitions.isEmpty()) {
				break;
			}

			dbMosaicDefinitions.stream().map(dbMosaicDefinition -> new MosaicDefinitionMetaDataPair(this.map(dbMosaicDefinition),
					new DefaultMetaData(dbMosaicDefinition.getId()))).forEach(pair -> {
						curDbId[0] = pair.getMetaData().getId();
						final NamespaceId namespaceId = pair.getEntity().getId().getNamespaceId();
						boolean test = this.mosaicInfoFactory.isNamespaceActive(namespaceId);
						if (pairs.size() < page.getPageSize() && this.mosaicInfoFactory.isNamespaceActive(namespaceId)) {
							pairs.add(pair);
						}
					});
		}

		return pairs;
	}
}
