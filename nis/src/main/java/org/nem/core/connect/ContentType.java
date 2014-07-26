package org.nem.core.connect;

import org.eclipse.jetty.http.MimeTypes;

/**
 * Static class containing content type constants.
 */
public class ContentType {

	/**
	 * The content type used for binary requests and responses.
	 */
	public static final String BINARY = "application/binary";

	/**
	 * The content type used for json requests and responses.
	 */
	public static final String JSON = MimeTypes.Type.APPLICATION_JSON.asString();

	/**
	 * The content type used for void responses.
	 */
	public static final String VOID = null;
}
