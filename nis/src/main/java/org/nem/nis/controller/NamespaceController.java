package org.nem.nis.controller;

import org.nem.core.model.namespace.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST namespace controller.
 */
@RestController
public class NamespaceController {
	private final NamespaceDao namespaceDao;
	private final ReadOnlyMosaicDao mosaicDao;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	NamespaceController(
			final NamespaceDao namespaceDao,
			final ReadOnlyMosaicDao mosaicDao,
			final NisDbModelToModelMapper mapper) {
		this.namespaceDao = namespaceDao;
		this.mosaicDao = mosaicDao;
		this.mapper = mapper;
	}

	//region getRoots

	/**
	 * Gets all known root namespaces.
	 *
	 * @param pageBuilder The page builder.
	 * @return All root namespaces.
	 */
	@RequestMapping(value = "/namespace/roots", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<NamespaceMetaDataPair> getRoots(final DefaultPageBuilder pageBuilder) {
		final DefaultPage page = pageBuilder.build();
		final Collection<DbNamespace> namespaces = this.namespaceDao.getRootNamespaces(page.getId(), page.getPageSize());
		final Collection<NamespaceMetaDataPair> pairs = namespaces.stream()
				.map(n -> new NamespaceMetaDataPair(
						this.mapper.map(n),
						new DefaultMetaData(n.getId())))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	//endregion

	//region getMosaicsForNamespace

	/**
	 * Gets all known mosaics for a namespace.
	 * TODO 20150709 J-B: not sure but i think this makes more sense in the mosaics controller
	 * TODO 20150711 BR -> J: i am undecided. For accounts we have the mosaic request in the account controller.
	 *
	 * @param pageBuilder The page builder.
	 * @return All known mosaics for the namespace.
	 */
	@RequestMapping(value = "/namespace/mosaics", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicMetaDataPair> getNamespaceMosaics(final NamespaceIdMaxIdPageBuilder pageBuilder) {
		final NamespaceIdMaxIdPage page = pageBuilder.build();
		final Collection<DbMosaic> mosaics = this.mosaicDao.getMosaicsForNamespace(page.getNamespaceId(), page.getId(), page.getPageSize());
		final Collection<MosaicMetaDataPair> pairs = mosaics.stream()
				.map(n -> new MosaicMetaDataPair(
						this.mapper.map(n),
						new DefaultMetaData(n.getId())))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	//endregion

	//region get

	/**
	 * Gets information about the specified namespace.
	 *
	 * @param builder The namespace id builder.
	 * @return All root namespaces.
	 */
	@RequestMapping(value = "/namespace/get", method = RequestMethod.GET)
	@ClientApi
	public Namespace get(final NamespaceIdBuilder builder) {
		final NamespaceId id = builder.build();
		final DbNamespace namespace = this.namespaceDao.getNamespace(id);
		if (null == namespace) {
			throw new MissingResourceException("invalid namespace", Namespace.class.getName(), id.toString());
		}

		return this.mapper.map(namespace);
	}

	//endregion
}
