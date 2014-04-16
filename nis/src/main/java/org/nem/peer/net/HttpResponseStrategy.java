package org.nem.peer.net;

import org.eclipse.jetty.client.api.*;

import java.io.IOException;

/**
 * Strategy for coercing an HTTP response into a specific type.
 *
 * @param <T> Type of response.
 */
public interface HttpResponseStrategy<T> {

	/**
	 * Coerces a result of type T given the specified request and response.
	 *
	 * @param request The request
	 * @param response The response.
	 * @return The coerced result.
	 */
	public T coerce(final Request request, final Response response) throws IOException;
}