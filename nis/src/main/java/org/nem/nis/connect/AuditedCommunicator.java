package org.nem.nis.connect;

import org.nem.core.serialization.*;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.audit.AuditCollection;
import org.nem.peer.connect.Communicator;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * An audited communicator decorator.
 */
public class AuditedCommunicator implements Communicator {
	private final Communicator communicator;
	private final AuditCollection auditCollection;

	/**
	 * Creates a new audited communicator.
	 *
	 * @param communicator The communicator to decorate.
	 * @param auditCollection The audit collection to use.
	 */
	public AuditedCommunicator(final Communicator communicator, final AuditCollection auditCollection) {
		this.communicator = communicator;
		this.auditCollection = auditCollection;
	}

	@Override
	public CompletableFuture<Deserializer> post(final URL url, final SerializableEntity entity) {
		return this.wrap(url, this.communicator.post(url, entity));
	}

	@Override
	public CompletableFuture<Deserializer> postVoid(final URL url, final SerializableEntity entity) {
		return this.wrap(url, this.communicator.postVoid(url, entity));
	}

	private CompletableFuture<Deserializer> wrap(final URL url, final CompletableFuture<Deserializer> future) {
		this.auditCollection.add(url.getHost(), url.getPath());
		return future.handle((d, e) -> {
			this.auditCollection.remove(url.getHost(), url.getPath());
			if (null != e) {
				ExceptionUtils.propagateVoid(() -> {
					throw (Exception) e;
				});
			}

			return d;
		});
	}
}
