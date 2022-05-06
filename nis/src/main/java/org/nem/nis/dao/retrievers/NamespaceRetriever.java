package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dao.HibernateUtils;
import org.nem.nis.dbmodel.DbNamespace;

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
	public Collection<DbNamespace> getNamespacesForAccount(final Session session, final long accountId, final NamespaceId parent,
			final int limit) {
		final HashMap<String, DbNamespace> rootMap = getCurrentRootNamespacesForAccount(session, accountId);

		// account must own root of parent
		if (null != parent && null == rootMap.get(parent.getRoot().toString())) {
			return Collections.emptyList();
		}

		final Criteria criteria = session.createCriteria(DbNamespace.class) // preserve-newline
				.add(Restrictions.eq("owner.id", accountId)) // preserve-newline
				.setMaxResults(limit);
		if (null != parent) {
			criteria.add(Restrictions.like("fullName", parent.toString() + ".", MatchMode.START));
		}

		final List<DbNamespace> dbNamespaces = HibernateUtils.listAndCast(criteria);
		final HashMap<String, DbNamespace> map = new HashMap<>();
		dbNamespaces.forEach(n -> {
			// note: hibernate will throw a StaleStateException upon flushing the session if we modify the original dbNamespace object
			final DbNamespace root = rootMap.get(extractRootName(n.getFullName()));
			final DbNamespace dbNamespace = new DbNamespace(n, root.getOwner(), root.getHeight());
			map.put(dbNamespace.getFullName(), dbNamespace);
		});

		return map.values();
	}

	/**
	 * Gets the specified namespace.
	 *
	 * @param session The session.
	 * @param id The namespace id.
	 * @return The specified namespace or null.
	 */
	public DbNamespace getNamespace(final Session session, final NamespaceId id) {
		final DbNamespace root = getCurrentRootNamespace(session, id);
		if (null == root) {
			return null;
		}

		final Criteria criteria = session.createCriteria(DbNamespace.class) // preserve-newline
				.add(Restrictions.eq("fullName", id.toString()));

		final List<DbNamespace> dbNamespaces = HibernateUtils.listAndCast(criteria);
		if (dbNamespaces.isEmpty()) {
			return null;
		}

		// note: hibernate will throw a StaleStateException upon flushing the session if we modify the original dbNamespace object
		return new DbNamespace(dbNamespaces.get(0), root.getOwner(), root.getHeight());
	}

	/**
	 * Retrieves all root namespaces.
	 *
	 * @param session The session.
	 * @param maxId The id of "top-most" namespace.
	 * @param limit The limit.
	 * @return The root namespaces.
	 */
	public Collection<DbNamespace> getRootNamespaces(final Session session, final long maxId, final int limit) {
		final String queryString = "SELECT n.* FROM namespaces n "
				+ "WHERE fullName NOT IN (SELECT fullName FROM namespaces WHERE id > n.id) " // preserve-newline
				+ "AND level = 0 " // preserve-newline
				+ "AND id < :maxId " // preserve-newline
				+ "ORDER BY id DESC " // preserve-newline
				+ "LIMIT :limit";
		final Query query = session.createSQLQuery(queryString) // preserve-newline
				.addEntity(DbNamespace.class) // preserve-newline
				.setParameter("maxId", maxId) // preserve-newline
				.setParameter("limit", limit);
		return HibernateUtils.listAndCast(query);
	}

	private static HashMap<String, DbNamespace> getCurrentRootNamespacesForAccount(final Session session, final Long accountId) {
		final Criteria criteria = session.createCriteria(DbNamespace.class) // preserve-newline
				.add(Restrictions.eq("owner.id", accountId)) // preserve-newline
				.add(Restrictions.eq("level", 0));
		final List<DbNamespace> roots = HibernateUtils.listAndCast(criteria);
		final HashMap<String, DbNamespace> map = new HashMap<>();
		roots.forEach(n -> {
			final DbNamespace current = map.get(n.getFullName());
			if (null == current || current.getHeight().compareTo(n.getHeight()) < 0) {
				map.put(n.getFullName(), n);
			}
		});

		return map;
	}

	private static DbNamespace getCurrentRootNamespace(final Session session, final NamespaceId id) {
		final String rootName = id.getRoot().toString();
		final Criteria criteria = session.createCriteria(DbNamespace.class) // preserve-newline
				.add(Restrictions.eq("fullName", rootName)) // preserve-newline
				.addOrder(Order.desc("height"));
		final List<DbNamespace> roots = HibernateUtils.listAndCast(criteria);
		return roots.isEmpty() ? null : roots.get(0);
	}

	private static String extractRootName(final String fullName) {
		final int index = fullName.indexOf('.');
		return -1 == index ? fullName : fullName.substring(0, index);
	}
}
