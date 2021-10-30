package org.nem.nis.controller;

import org.nem.core.model.namespace.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyNamespaceDao;
import org.nem.nis.dbmodel.DbNamespace;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.MosaicInfoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * REST namespace controller.
 */
@RestController
public class NamespaceController {
	private final ReadOnlyNamespaceDao namespaceDao;
	private final NisDbModelToModelMapper mapper;
	private final MosaicInfoFactory mosaicInfoFactory;

	@Autowired(required = true)
	NamespaceController(final ReadOnlyNamespaceDao namespaceDao, final NisDbModelToModelMapper mapper,
			final MosaicInfoFactory mosaicInfoFactory) {
		this.namespaceDao = namespaceDao;
		this.mapper = mapper;
		this.mosaicInfoFactory = mosaicInfoFactory;
	}

	// region getRoots

	/**
	 * Gets all known root namespaces.
	 *
	 * @param pageBuilder The page builder.
	 * @return All root namespaces.
	 */
	@RequestMapping(value = "/namespace/root/page", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<NamespaceMetaDataPair> getRoots(final DefaultPageBuilder pageBuilder) {
		final DefaultPage page = pageBuilder.build();
		final Collection<NamespaceMetaDataPair> pairs = recursivelyGetNamespaces(this.namespaceDao::getRootNamespaces, page);
		return new SerializableList<>(pairs);
	}

	// endregion

	// region get

	/**
	 * Gets information about the specified namespace.
	 *
	 * @param builder The namespace id builder.
	 * @return All root namespaces.
	 */
	@RequestMapping(value = "/namespace", method = RequestMethod.GET)
	@ClientApi
	public Namespace get(final NamespaceIdBuilder builder) {
		final NamespaceId id = builder.build();
		if (!this.mosaicInfoFactory.isNamespaceActive(id)) {
			throw new MissingResourceException("invalid namespace", Namespace.class.getName(), id.toString());
		}

		final DbNamespace namespace = this.namespaceDao.getNamespace(id);
		if (null == namespace) {
			throw new MissingResourceException("invalid namespace", Namespace.class.getName(), id.toString());
		}

		return this.map(namespace);
	}

	// endregion

	// region account/namespaces

	/**
	 * Gets information about an account's namespaces.
	 *
	 * @param idBuilder The account namespace builder.
	 * @param pageBuilder The page builder.
	 * @return Information about the namespaces owned by an account.
	 */
	@RequestMapping(value = "/account/namespace/page", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Namespace> accountNamespaces(final AccountNamespaceBuilder idBuilder, final DefaultPageBuilder pageBuilder) {
		final DefaultPage page = pageBuilder.build();
		final AccountNamespace accountNamespace = idBuilder.build();

		final Collection<DbNamespace> dbNamespaces = this.namespaceDao.getNamespacesForAccount(accountNamespace.getAddress(),
				accountNamespace.getParent(), page.getPageSize());
		return new SerializableList<>(dbNamespaces.stream().map(this::map).filter(n -> this.mosaicInfoFactory.isNamespaceActive(n.getId()))
				.collect(Collectors.toList()));
	}

	// endregion

	private Namespace map(final DbNamespace dbNamespace) {
		return this.mapper.map(dbNamespace, Namespace.class);
	}

	private Collection<NamespaceMetaDataPair> recursivelyGetNamespaces(final BiFunction<Long, Integer, Collection<DbNamespace>> retriever,
			final DefaultPage page) {
		Long[] curDbId = {
				page.getId()
		};
		final Collection<NamespaceMetaDataPair> pairs = new ArrayList<>();
		while (pairs.size() < page.getPageSize()) {
			final Collection<DbNamespace> dbNamespaces = retriever.apply(curDbId[0], page.getPageSize());
			if (dbNamespaces.isEmpty()) {
				break;
			}

			dbNamespaces.stream()
					.map(dbNamespace -> new NamespaceMetaDataPair(this.map(dbNamespace), new DefaultMetaData(dbNamespace.getId())))
					.forEach(pair -> {
						curDbId[0] = pair.getMetaData().getId();
						final NamespaceId namespaceId = pair.getEntity().getId();
						boolean test = this.mosaicInfoFactory.isNamespaceActive(namespaceId);
						if (pairs.size() < page.getPageSize() && this.mosaicInfoFactory.isNamespaceActive(namespaceId)) {
							pairs.add(pair);
						}
					});
		}

		return pairs;
	}
}
