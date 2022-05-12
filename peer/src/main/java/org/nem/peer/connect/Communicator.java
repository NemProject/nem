package org.nem.peer.connect;

import org.nem.core.serialization.*;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy for posting information to a remote url.
 */
public interface Communicator {
	/**
	 * Posts the entity to the url.
	 *
	 * @param url The url.
	 * @param entity the entity.
	 * @return A deserializer wrapping the response.
	 */
	CompletableFuture<Deserializer> post(final URL url, final SerializableEntity entity);

	/**
	 * Posts the entity to the url.
	 *
	 * @param url The url.
	 * @param entity the entity.
	 * @return A deserializer wrapping the (void) response.
	 */
	CompletableFuture<Deserializer> postVoid(final URL url, final SerializableEntity entity);
}
