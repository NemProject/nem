package org.nem.nis.dao;

import org.hibernate.Session;

public class DbUtils {
	public static void dbCleanup(final Session session1) {
		session1.createSQLQuery("delete from multisigsignatures").executeUpdate();
		session1.createSQLQuery("delete from multisigtransactions").executeUpdate();
		session1.createSQLQuery("delete from transfers").executeUpdate();
		session1.createSQLQuery("delete from importancetransfers").executeUpdate();
		session1.createSQLQuery("delete from multisigmodifications").executeUpdate();
		session1.createSQLQuery("delete from multisigsignermodifications").executeUpdate();
		session1.createSQLQuery("delete from multisigsends").executeUpdate();
		session1.createSQLQuery("delete from multisigreceives").executeUpdate();
		session1.createSQLQuery("delete from blocks").executeUpdate();
		session1.createSQLQuery("delete from accounts").executeUpdate();
		session1.createSQLQuery("ALTER SEQUENCE transaction_id_seq RESTART WITH 1").executeUpdate();
		session1.createSQLQuery("ALTER TABLE multisigmodifications ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session1.createSQLQuery("ALTER TABLE multisigsends ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session1.createSQLQuery("ALTER TABLE multisigreceives ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session1.createSQLQuery("ALTER TABLE blocks ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session1.createSQLQuery("ALTER TABLE accounts ALTER COLUMN id RESTART WITH 1").executeUpdate();

		session1.flush();
		session1.clear();
	}
}
