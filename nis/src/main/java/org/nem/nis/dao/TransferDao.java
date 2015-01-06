package org.nem.nis.dao;

import org.nem.nis.dbmodel.DbTransferTransaction;

/**
 * DAO for accessing db transfer objects.
 */
public interface TransferDao extends ReadOnlyTransferDao, SimpleTransferDao<DbTransferTransaction> {
}