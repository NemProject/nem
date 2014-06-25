package org.nem.peer.connect;

import org.nem.core.connect.*;
import org.nem.core.serialization.*;
import org.nem.nis.audit.AuditCollection;

/**
 * A factory of PeerConnector and SyncConnector objects that enables the flyweight pattern
 * (where HttpMethodClient is the shared resource).
 */
public class HttpConnectorPool implements SyncConnectorPool {
	private final HttpMethodClient<Deserializer> httpMethodClient;
	private final AuditCollection auditCollection;

	/**
	 * Creates a new HTTP connector pool.
	 *
	 * @param auditCollection The audit collection.
	 */
	public HttpConnectorPool(final AuditCollection auditCollection) {
		this.httpMethodClient = new HttpMethodClient<>();
		this.auditCollection = auditCollection;
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

	private HttpConnector getConnector(final AccountLookup accountLookup) {
		final DeserializationContext context = new DeserializationContext(accountLookup);
		final HttpDeserializerResponseStrategy strategy = new HttpDeserializerResponseStrategy(context);
		final HttpVoidResponseStrategy voidStrategy = new HttpVoidResponseStrategy();
		final Communicator communicator = new HttpCommunicator(this.httpMethodClient, strategy, voidStrategy);
		return new HttpConnector(new AuditedCommunicator(communicator, this.auditCollection));
	}
}
