package org.nem.nis.controller;

import org.nem.core.model.namespace.*;
import org.nem.core.serialization.*;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.NamespaceIdBuilder;
import org.nem.nis.dao.NamespaceDao;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST namespace controller.
 */
@RestController
public class NamespaceController {
	// TODO 20150623 J-J: probably need to make this configurable
	private static final int DEFAULT_LIMIT = 25;

	private final NamespaceDao namespaceDao;
	private final IMapper mapper;

	@Autowired(required = true)
	NamespaceController(
			final NamespaceDao namespaceDao,
			final MapperFactory mapperFactory,
			final AccountLookup accountLookup) {
		this.namespaceDao = namespaceDao;
		this.mapper = mapperFactory.createDbModelToModelMapper(accountLookup);
	}

	//region getRoots

	/**
	 * Gets all known root namespaces.
	 *
	 * @return All root namespaces.
	 */
	@RequestMapping(value = "/namespace/roots", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Namespace> getRoots() {
		final Collection<Namespace> namespaces = this.namespaceDao.getRootNamespaces(DEFAULT_LIMIT).stream()
				.map(n -> this.mapper.map(n, Namespace.class))
				.collect(Collectors.toList());
		return new SerializableList<>(namespaces);
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

		return this.mapper.map(namespace, Namespace.class);
	}

	//endregion
}
