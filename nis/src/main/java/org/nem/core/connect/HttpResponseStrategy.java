package org.nem.core.connect;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

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
	T coerce(final HttpRequestBase request, final HttpResponse response);

	/**
	 * Gets the supported content type.
	 *
	 * @return The supported content type.
	 */
	String getSupportedContentType();
}