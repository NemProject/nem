package org.nem.nis.test;

import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.dbmodel.*;

/**
 * Static class containing helper functions for dbmodel related tests.
 */
public class DbModelTestUtils {

	/**
	 * Creates a transfer db model.
	 *
	 * @param dbModelClass The transfer db model class.
	 * @param <T> The transfer db model type.
	 * @return The created transfer db model.
	 */
	public static <T extends AbstractBlockTransfer> T createTransferDbModel(final Class<T> dbModelClass) {
		final T dbTransfer = ExceptionUtils.propagate(dbModelClass::newInstance);

		// initialize any derived required fields
		if (dbModelClass.equals(DbProvisionNamespaceTransaction.class)) {
			((DbProvisionNamespaceTransaction)dbTransfer).setNamespace(new DbNamespace());
		}

		return dbTransfer;
	}
}
