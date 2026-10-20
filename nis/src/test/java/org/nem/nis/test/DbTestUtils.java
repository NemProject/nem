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
		session.createNativeQuery("delete from multisigsignatures").executeUpdate();
		session.createNativeQuery("delete from multisigtransactions").executeUpdate();
		session.createNativeQuery("delete from transferredmosaics").executeUpdate();
		session.createNativeQuery("delete from transfers").executeUpdate();
		session.createNativeQuery("delete from importancetransfers").executeUpdate();
		session.createNativeQuery("delete from multisigmodifications").executeUpdate();
		session.createNativeQuery("delete from multisigsignermodifications").executeUpdate();
		session.createNativeQuery("delete from mincosignatoriesmodifications").executeUpdate();
		session.createNativeQuery("delete from multisigsends").executeUpdate();
		session.createNativeQuery("delete from multisigreceives").executeUpdate();
		session.createNativeQuery("delete from namespaceprovisions").executeUpdate();
		session.createNativeQuery("delete from namespaces").executeUpdate();
		session.createNativeQuery("delete from mosaicdefinitioncreationtransactions").executeUpdate();
		session.createNativeQuery("delete from mosaicproperties").executeUpdate();
		session.createNativeQuery("delete from mosaicdefinitions").executeUpdate();
		session.createNativeQuery("delete from mosaicsupplychanges").executeUpdate();
		session.createNativeQuery("delete from blocks").executeUpdate();
		session.createNativeQuery("delete from accounts").executeUpdate();
		session.createNativeQuery("ALTER SEQUENCE transaction_id_seq RESTART WITH 1").executeUpdate();
		session.createNativeQuery("ALTER TABLE multisigmodifications ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createNativeQuery("ALTER TABLE multisigsends ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createNativeQuery("ALTER TABLE multisigreceives ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createNativeQuery("ALTER TABLE namespaces ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createNativeQuery("ALTER TABLE mosaicproperties ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createNativeQuery("ALTER TABLE mosaicdefinitions ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createNativeQuery("ALTER TABLE transferredmosaics ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createNativeQuery("ALTER TABLE blocks ALTER COLUMN id RESTART WITH 1").executeUpdate();
		session.createNativeQuery("ALTER TABLE accounts ALTER COLUMN id RESTART WITH 1").executeUpdate();

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
