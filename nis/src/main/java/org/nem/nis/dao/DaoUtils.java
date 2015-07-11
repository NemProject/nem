package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.type.LongType;
import org.nem.core.model.*;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Helper class containing functions to facilitate working with dao classes.
 * TODO 20150709 J-B: should add a unit test
 * TODO 20150711 BR -> J: added simple tests.
 */
public class DaoUtils {

	/**
	 * Gets the account id for a given account.
	 *
	 * @param session The session.
	 * @param account the account.
	 * @return The account id.
	 */
	public static Long getAccountId(final Session session, final Account account) {
		final Address address = account.getAddress();
		final Query query = session
				.createSQLQuery("select id as accountId from accounts WHERE printablekey=:address")
				.addScalar("accountId", LongType.INSTANCE)
				.setParameter("address", address.getEncoded());
		return (Long)query.uniqueResult();
	}

	/**
	 * Gets the account id for a given addresses.
	 *
	 * @param session The session.
	 * @param addresses the addresses.
	 * @return The account ids.
	 */
	public static Collection<Long> getAccountIds(final Session session, final Collection<Address> addresses) {
		final Query query = session
				.createSQLQuery("SELECT id AS accountId FROM accounts WHERE printableKey in (:addresses)")
				.addScalar("accountId", LongType.INSTANCE)
				.setParameterList("addresses", addresses.stream().map(Address::toString).collect(Collectors.toList()));
		return HibernateUtils.listAndCast(query);
	}
}
