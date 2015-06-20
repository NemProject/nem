package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dao.HibernateUtils;
import org.nem.nis.dbmodel.DbNamespace;

import java.util.Collection;

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
		final Criteria criteria = session.createCriteria(DbNamespace.class)
				.add(Restrictions.eq("owner.id", accountId))
				.setMaxResults(limit)
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		if (null != parent) {
			criteria.add(Restrictions.like("fullName", parent.toString(), MatchMode.START));
		}

		return HibernateUtils.listAndCast(criteria);
	}

	/**
	 * Gets the specified namespace.
	 *
	 * @param session The session.
	 * @param fullName The fully qualified namespace name.
	 * @return The specified namespace or null.
	 */
	public Collection<DbNamespace> getNamespace(
			final Session session,
			final String fullName) {
		final Criteria criteria = session.createCriteria(DbNamespace.class)
				.add(Restrictions.eq("fullName", fullName))
				.addOrder(Order.desc("expiryHeight"))
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		return HibernateUtils.listAndCast(criteria);
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
				.addOrder(Order.desc("expiryHeight"))
				.setMaxResults(limit)
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		return HibernateUtils.listAndCast(criteria);
	}
}
