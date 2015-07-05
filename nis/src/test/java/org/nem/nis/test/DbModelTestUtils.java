package org.nem.nis.test;

import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.dbmodel.*;

import java.util.concurrent.Callable;

/**
 * Static class containing helper functions for dbmodel related tests.
 */
public class DbModelTestUtils {

	public static <T extends AbstractBlockTransfer> T createTransferDbModel(final Class<T> dbModelClass) {
		final T dbTransfer = ExceptionUtils.propagate((Callable<T>)dbModelClass::newInstance);

		// initialize any derived required fields
		if (dbModelClass.equals(DbProvisionNamespaceTransaction.class)) {
			((DbProvisionNamespaceTransaction)dbTransfer).setNamespace(new DbNamespace());
		}

		return dbTransfer;
	}
}
