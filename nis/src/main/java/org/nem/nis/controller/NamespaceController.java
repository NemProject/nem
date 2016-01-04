package org.nem.nis.controller;

import org.nem.core.model.namespace.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyNamespaceDao;
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
	private final ReadOnlyNamespaceDao namespaceDao;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	NamespaceController(
			final ReadOnlyNamespaceDao namespaceDao,
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
	@RequestMapping(value = "/namespace/root/page", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<NamespaceMetaDataPair> getRoots(final DefaultPageBuilder pageBuilder) {
		final DefaultPage page = pageBuilder.build();
		final Collection<DbNamespace> namespaces = this.namespaceDao.getRootNamespaces(page.getId(), page.getPageSize());
		final Collection<NamespaceMetaDataPair> pairs = namespaces.stream()
				.map(n -> new NamespaceMetaDataPair(
						this.map(n),
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
	@RequestMapping(value = "/namespace", method = RequestMethod.GET)
	@ClientApi
	public Namespace get(final NamespaceIdBuilder builder) {
		final NamespaceId id = builder.build();
		final DbNamespace namespace = this.namespaceDao.getNamespace(id);
		if (null == namespace) {
			throw new MissingResourceException("invalid namespace", Namespace.class.getName(), id.toString());
		}

		return this.map(namespace);
	}

	//endregion

	//region account/namespaces

	/**
	 * Gets information about an account's namespaces.
	 *
	 * @param idBuilder The account namespace builder.
	 * @param pageBuilder The page builder.
	 * @return Information about the namespaces owned by an account.
	 */
	@RequestMapping(value = "/account/namespace/page", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Namespace> accountNamespaces(
			final AccountNamespaceBuilder idBuilder,
			final DefaultPageBuilder pageBuilder) {
		final DefaultPage page = pageBuilder.build();
		final AccountNamespace accountNamespace = idBuilder.build();
		final Collection<DbNamespace> namespaces = this.namespaceDao.getNamespacesForAccount(
				accountNamespace.getAddress(),
				accountNamespace.getParent(),
				page.getPageSize());
		return new SerializableList<>(namespaces.stream().map(this::map).collect(Collectors.toList()));
	}

	//endregion

	private Namespace map(final DbNamespace dbNamespace) {
		return this.mapper.map(dbNamespace, Namespace.class);
	}
}
