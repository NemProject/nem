package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dao.HibernateUtils;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * Class for for retrieving namespaces for a given account.
 */
public class NamespaceRetriever {

	/**
	 * Gets namespaces for the specified account.
	 *
	 * @param session The session.
	 * @param accountId The account identifier.
	 * @param parent The optional parent namespace.
	 * @param limit The limit.
	 * @return The namespaces.
	 */
	public Collection<DbNamespace> getNamespacesForAccount(
			final Session session,
			final long accountId,
			final NamespaceId parent,
			final int limit) {
		final HashMap<String, DbNamespace> rootMap = getCurrentRootNamespacesForAccount(session, accountId);

		// account must own root of parent
		if (null != parent && null == rootMap.get(parent.getRoot().toString())) {
			return Collections.emptyList();
		}

		final Criteria criteria = session.createCriteria(DbNamespace.class)
				.add(Restrictions.eq("owner.id", accountId))
				.setMaxResults(limit);
		if (null != parent) {
			criteria.add(Restrictions.like("fullName", parent.toString() + ".", MatchMode.START));
		}

		final List<DbNamespace> dbNamespaces = HibernateUtils.listAndCast(criteria);
		final HashMap<String, DbNamespace> map = new HashMap<>();
		dbNamespaces.stream().forEach(n -> {
			// note: hibernate will throw a StaleStateException upon flushing the session if we modify the original dbNamespace object
			final DbNamespace root = rootMap.get(extractRootName(n.getFullName()));
			final DbNamespace dbNamespace = this.createModifiedDbNamespace(
					n,
					root.getOwner(),
					root.getHeight());
			map.put(dbNamespace.getFullName(), dbNamespace);
		});

		return map.values();
	}

	/**
	 * Gets the specified namespace.
	 *
	 * @param session The session.
	 * @param fullName The fully qualified namespace name.
	 * @return The specified namespace or null.
	 */
	public DbNamespace getNamespace(
			final Session session,
			final String fullName) {
		final DbNamespace root = getCurrentRootNamespace(session, fullName);
		if (null == root) {
			return null;
		}

		final Criteria criteria = session.createCriteria(DbNamespace.class)
				.add(Restrictions.eq("fullName", fullName));

		final List<DbNamespace> dbNamespaces = HibernateUtils.listAndCast(criteria);
		if (dbNamespaces.isEmpty()) {
			return null;
		}

		// note: hibernate will throw a StaleStateException upon flushing the session if we modify the original dbNamespace object
		return this.createModifiedDbNamespace(
				dbNamespaces.get(0),
				root.getOwner(),
				root.getHeight());
	}

	/**
	 * Retrieves all root namespaces.
	 *
	 * @param session The session.
	 * @param limit The limit.
	 * @return The root namespaces.
	 */
	public Collection<DbNamespace> getRootNamespaces(
			final Session session,
			final int limit) {
		final Criteria criteria = session.createCriteria(DbNamespace.class)
				.add(Restrictions.eq("level", 0))
				.addOrder(Order.asc("fullName"))
				.addOrder(Order.asc("height"))
				.setMaxResults(limit);

		final List<DbNamespace> dbNamespaces =  HibernateUtils.listAndCast(criteria);
		final HashMap<String, DbNamespace> map = new HashMap<>();
		dbNamespaces.stream().forEach(n -> map.put(n.getFullName(), n));
		return map.values();
	}

	private static HashMap<String, DbNamespace> getCurrentRootNamespacesForAccount(final Session session, final Long accountId) {
		final Criteria criteria = session.createCriteria(DbNamespace.class)
				.add(Restrictions.eq("owner.id", accountId))
				.add(Restrictions.eq("level", 0));
		final List<DbNamespace> roots = HibernateUtils.listAndCast(criteria);
		final HashMap<String, DbNamespace> map = new HashMap<>();
		roots.stream().forEach(n -> {
			final DbNamespace current = map.get(n.getFullName());
			if ((null == current) ||
				current.getHeight().compareTo(n.getHeight()) < 0) {
				map.put(n.getFullName(), n);
			}
		});

		return map;
	}

	private static DbNamespace getCurrentRootNamespace(final Session session, final String fullName) {
		final String rootName = extractRootName(fullName);
		final Criteria criteria = session.createCriteria(DbNamespace.class)
				.add(Restrictions.eq("fullName", rootName))
				.addOrder(Order.desc("height"));
		final List<DbNamespace> roots = HibernateUtils.listAndCast(criteria);
		return roots.isEmpty() ? null : roots.get(0);
	}

	private static String extractRootName(final String fullName) {
		final int index = fullName.indexOf('.');
		return -1 == index ? fullName : fullName.substring(0, index);
	}

	private DbNamespace createModifiedDbNamespace(
			final DbNamespace original,
			final DbAccount owner,
			final Long height) {
		final DbNamespace dbNamespace = new DbNamespace();
		dbNamespace.setId(original.getId());
		dbNamespace.setFullName(original.getFullName());
		dbNamespace.setOwner(owner);
		dbNamespace.setHeight(height);
		dbNamespace.setLevel(original.getLevel());
		return dbNamespace;
	}
}
