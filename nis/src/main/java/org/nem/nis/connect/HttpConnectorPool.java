package org.nem.nis.connect;

import org.nem.core.connect.HttpMethodClient;
import org.nem.core.serialization.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.time.synchronization.TimeSynchronizationConnector;
import org.nem.peer.connect.*;

/**
 * A factory of PeerConnector and SyncConnector objects that enables the flyweight pattern (where HttpMethodClient is the shared resource).
 */
public class HttpConnectorPool implements SyncConnectorPool {
	private final CommunicationMode communicationMode;
	private final AuditCollection auditCollection;
	private final HttpMethodClient<Deserializer> httpMethodClient;

	/**
	 * Creates a new HTTP connector pool.
	 *
	 * @param communicationMode The communication mode.
	 * @param auditCollection The audit collection.
	 */
	public HttpConnectorPool(final CommunicationMode communicationMode, final AuditCollection auditCollection) {
		this.communicationMode = communicationMode;
		this.auditCollection = auditCollection;
		this.httpMethodClient = new HttpMethodClient<>();
	}

	@Override
	public SyncConnector getSyncConnector(final AccountLookup accountLookup) {
		return this.getConnector(accountLookup);
	}

	/**
	 * Gets a PeerConnector instance.
	 *
	 * @param accountLookup The account lookup to associate with the connector.
	 * @return The connector.
	 */
	public PeerConnector getPeerConnector(final AccountLookup accountLookup) {
		return this.getConnector(accountLookup);
	}

	/**
	 * Gets a TimeSynchronizationConnector instance.
	 *
	 * @param accountLookup The account lookup to associate with the connector.
	 * @return The connector.
	 */
	public TimeSynchronizationConnector getTimeSyncConnector(final AccountLookup accountLookup) {
		return this.getConnector(accountLookup);
	}

	private HttpConnector getConnector(final AccountLookup accountLookup) {
		final DeserializationContext context = new DeserializationContext(accountLookup);
		final Communicator communicator = new HttpCommunicator(this.httpMethodClient, this.communicationMode, context);
		return new HttpConnector(new AuditedCommunicator(communicator, this.auditCollection));
	}
}
