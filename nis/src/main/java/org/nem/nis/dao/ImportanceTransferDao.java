package org.nem.nis.dao;

import org.nem.nis.dbmodel.DbImportanceTransferTransaction;

/**
 * DAO for accessing db importance transfer objects.
 */
public interface ImportanceTransferDao extends SimpleReadOnlyTransferDao<DbImportanceTransferTransaction>, SimpleTransferDao<DbImportanceTransferTransaction> {
}
