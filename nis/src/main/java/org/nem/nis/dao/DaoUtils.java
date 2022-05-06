package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.type.LongType;
import org.nem.core.model.Address;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Helper class containing functions to facilitate working with dao classes.
 */
public class DaoUtils {

	/**
	 * Gets the account id for a given address.
	 *
	 * @param session The session.
	 * @param address The address.
	 * @return The account id.
	 */
	public static Long getAccountId(final Session session, final Address address) {
		final Query query = session.createSQLQuery("select id as accountId from accounts WHERE printablekey=:address") // preserve-newline
				.addScalar("accountId", LongType.INSTANCE) // preserve-newline
				.setParameter("address", address.getEncoded());
		return (Long) query.uniqueResult();
	}

	/**
	 * Gets the account ids for given addresses.
	 *
	 * @param session The session.
	 * @param addresses The addresses.
	 * @return The account ids.
	 */
	public static Collection<Long> getAccountIds(final Session session, final Collection<Address> addresses) {
		final Query query = session.createSQLQuery("SELECT id AS accountId FROM accounts WHERE printableKey in (:addresses)")
				.addScalar("accountId", LongType.INSTANCE) // preserve-newline
				.setParameterList("addresses", addresses.stream().map(Address::toString).collect(Collectors.toList()));
		return HibernateUtils.listAndCast(query);
	}
}
