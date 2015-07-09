package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.type.LongType;
import org.nem.core.model.*;

/**
 * Helper class containing functions to facilitate working with dao classes.
 * TODO 20150709 J-B: i think we can replace similar code in the other (block + transaction) daos with this
 * TODO 20150709 J-B: should add a unit test
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
}
