package org.nem.nis.dao;

import java.util.List;
import org.hibernate.*;

/**
 * Helper class containing hibernate utility functions.
 */
public class HibernateUtils {

	/**
	 * Calls list on query and casts the result.
	 *
	 * @param query The query.
	 * @param <T> The result entity type.
	 * @return The typed list.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> listAndCast(final Query query) {
		return (List<T>) query.list();
	}

	/**
	 * Calls list on criteria and casts the result.
	 *
	 * @param criteria The criteria.
	 * @param <T> The result entity type.
	 * @return The typed list.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> listAndCast(final Criteria criteria) {
		return (List<T>) criteria.list();
	}
}
