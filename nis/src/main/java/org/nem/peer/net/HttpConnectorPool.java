package org.nem.peer.net;

import org.nem.core.serialization.*;
import org.nem.peer.*;

/**
 * A factory of PeerConnector and SyncConnector objects that enables the flyweight pattern
 * (where HttpMethodClient is the shared resource).
 */
public class HttpConnectorPool implements SyncConnectorPool {

	private static final int DEFAULT_TIMEOUT = 30;

	private final HttpMethodClient<Deserializer> httpMethodClient;

	/**
	 * Creates a new HTTP connector pool.
	 */
	public HttpConnectorPool() {
		this.httpMethodClient = new HttpMethodClient<>(DEFAULT_TIMEOUT);
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
		return new HttpConnector(this.httpMethodClient, strategy);
	}
}
