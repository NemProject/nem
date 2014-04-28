package org.nem.nis.service;

import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.dao.TransferDao;

/**
 * This is dummy interface to distinguish between "requiring" block DAO and raw DAO
 *
 * note: as for now it simply extends TransferDao, not sure if we need ReadOnlyTransferDao
 */
public interface RequiredTransferDao extends ReadOnlyTransferDao {
}
