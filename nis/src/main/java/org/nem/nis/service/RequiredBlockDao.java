package org.nem.nis.service;

import org.nem.nis.dao.BlockDao;
import org.nem.nis.dao.ReadOnlyBlockDao;

/**
 * This is dummy interface to distinguish between "requiring" block DAO and raw DAO
 */
public interface RequiredBlockDao extends ReadOnlyBlockDao {
}
