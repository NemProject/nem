package org.nem.nis.test;

import org.hibernate.Session;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.cache.*;
import org.nem.nis.dbmodel.*;

/**
 * Static class containing helper functions for db related tests.
 */
public class DbTestUtils {

	/**
	 * Creates a transfer db model.
	 *
	 * @param dbModelClass The transfer db model class.
	 * @param <T> The transfer db model type.
	 * @return The created transfer db model.
	 */
	@SuppressWarnings({
			"deprecation", "rawtypes"
	})
	public static <T extends AbstractBlockTransfer> T createTransferDbModel(final Class<T> dbModelClass) {
		final T dbTransfer = ExceptionUtils.propagate(dbModelClass::newInstance);

		// initialize any derived required fields
		if (dbModelClass.equals(DbProvisionNamespaceTransaction.class)) {
			((DbProvisionNamespaceTransaction) dbTransfer).setNamespace(new DbNamespace());
		}

		return dbTransfer;
	}

	/**
	 * Cleans up the database.
	 *
	 * @param session The session.
	 */
	public static void dbCleanup(final Session session) {
		session.createSQLQuery("delete from multisigsignatures").executeUpdate();
		session.createSQLQuery("delete from multisigtransactions").executeUpdate();
		session.createSQLQuery("delete from transferredmosaics").executeUpdate();
		session.createSQLQuery("delete from transfers").executeUpdate();
		session.createSQLQuery("delete from importancetransfers").executeUpdate();
		session.createSQLQuery("delete from multisigmodifications").executeUpdate();
		session.createSQLQuery("delete from multisigsignermodifications").executeUpdate();
		session.createSQLQuery("delete from mincosignatoriesmodifications").executeUpdate();
		session.createSQLQuery("delete from multisigsends").executeUpdate();
		session.createSQLQuery("delete from multisigreceives").executeUpdate();
		session.createSQLQuery("delete from namespaceprovisions").executeUpdate();
		session.createSQLQuery("delete from namespaces").executeUpdate();
		session.createSQLQuery("delete from mosaicdefinitioncreationtransactions").executeUpdate();
		session.createSQLQuery("delete from mosaicproperties").executeUpdate();
		session.createSQLQuery("delete from mosaicdefinitions").executeUpdate();
		session.createSQLQuery("delete from mosaicsupplychanges").executeUpdate();
		session.createSQLQuery("delete from blocks").executeUpdate();
		session.createSQLQuery("delete from accounts").executeUpdate();
		session.createSQLQuery("ALTER SEQUENCE transaction_id_seq RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE multisigmodifications ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE multisigsends ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE multisigreceives ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE namespaces ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE mosaicproperties ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE mosaicdefinitions ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE transferredmosaics ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE blocks ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createSQLQuery("ALTER TABLE accounts ALTER COLUMN id RESTART WITH 1").executeUpdate();

		session.flush();
		session.clear();
	}

	/**
	 * Cleans up the cache.
	 *
	 * @param cache The cache.
	 */
	public static void cacheCleanup(final SynchronizedAccountStateCache cache) {
		final SynchronizedAccountStateCache mutableCache = cache.copy();
		mutableCache.contents().stream().forEach(a -> mutableCache.removeFromCache(a.getAddress()));
		mutableCache.commit();
	}
}
