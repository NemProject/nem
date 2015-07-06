package org.nem.nis.dao;

import org.hibernate.Session;

/**
 * Test database utilities.
 */
public class DbUtils {

	/**
	 * Cleans up the database.
	 *
	 * @param session The session.
	 */
	public static void dbCleanup(final Session session) {
		session.createSQLQuery("delete from multisigsignatures").executeUpdate();
		session.createSQLQuery("delete from multisigtransactions").executeUpdate();
		session.createSQLQuery("delete from transfers").executeUpdate();
		session.createSQLQuery("delete from importancetransfers").executeUpdate();
		session.createSQLQuery("delete from multisigmodifications").executeUpdate();
		session.createSQLQuery("delete from multisigsignermodifications").executeUpdate();
		session.createSQLQuery("delete from mincosignatoriesmodifications").executeUpdate();
		session.createSQLQuery("delete from multisigsends").executeUpdate();
		session.createSQLQuery("delete from multisigreceives").executeUpdate();
		session.createSQLQuery("delete from namespaceprovisions").executeUpdate();
		session.createSQLQuery("delete from namespaces").executeUpdate();
		session.createSQLQuery("delete from mosaiccreationTransactions").executeUpdate();
		session.createSQLQuery("delete from mosaicproperties").executeUpdate();
		session.createSQLQuery("delete from mosaics").executeUpdate();
		session.createSQLQuery("delete from blocks").executeUpdate();
		session.createSQLQuery("delete from accounts").executeUpdate();
		session.createSQLQuery("ALTER SEQUENCE transaction_id_seq RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE multisigmodifications ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE multisigsends ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE multisigreceives ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE namespaces ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE mosaicproperties ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE mosaics ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE blocks ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE accounts ALTER COLUMN id RESTART WITH 1").executeUpdate();

		session.flush();
		session.clear();
	}
}
