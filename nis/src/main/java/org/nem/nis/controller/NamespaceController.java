package org.nem.nis.controller;

import org.nem.core.model.namespace.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.NamespaceDao;
import org.nem.nis.dbmodel.DbNamespace;
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
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	NamespaceController(
			final NamespaceDao namespaceDao,
			final NisDbModelToModelMapper mapper) {
		this.namespaceDao = namespaceDao;
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
	public SerializableList<NamespaceMetaDataPair> getRoots(final NamespacePageBuilder pageBuilder) {
		final NamespacePage page = pageBuilder.build();
		final Collection<DbNamespace> namespaces = this.namespaceDao.getRootNamespaces(page.getId(), page.getPageSize());
		final Collection<NamespaceMetaDataPair> pairs = namespaces.stream()
				.map(n -> new NamespaceMetaDataPair(
						this.mapper.map(n),
						new NamespaceMetaData(n.getId())))
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
