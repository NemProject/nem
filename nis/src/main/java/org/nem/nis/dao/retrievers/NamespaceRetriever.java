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
}
