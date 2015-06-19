package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.core.model.namespace.*;
import org.nem.nis.dao.HibernateUtils;
import org.nem.nis.dbmodel.DbNamespace;

import java.util.Collection;

/**
 * Class for for retrieving namespaces for a given account.
 */
public class NamespaceRetriever {

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

	public Collection<DbNamespace> getNamespace(
			final Session session,
			final String fullName) {
		final Criteria criteria = session.createCriteria(DbNamespace.class)
				.add(Restrictions.eq("fullName", fullName))
				.addOrder(Order.desc("expiryHeight"))
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		return HibernateUtils.listAndCast(criteria);
	}

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
