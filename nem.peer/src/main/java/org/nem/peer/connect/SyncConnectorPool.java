package org.nem.peer.connect;

import org.nem.core.serialization.AccountLookup;

/**
 * Sync connector pool.
 */
public interface SyncConnectorPool {

	/**
	 * Gets a SyncConnector instance.
	 *
	 * @param accountLookup The account lookup to associate with the connector.
	 * @return The connector.
	 */
	SyncConnector getSyncConnector(final AccountLookup accountLookup);
}
