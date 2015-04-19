package org.nem.core.metadata;

import java.net.URL;
import java.security.CodeSource;
import java.security.cert.*;
import java.util.logging.Logger;

/**
 * A facade around a code source.
 */
public class CodeSourceFacade {
	private static final Logger LOGGER = Logger.getLogger(CodeSourceFacade.class.getName());

	private final URL location;
	private final X509Certificate firstCertificate;

	/**
	 * Creates a new code source.
	 *
	 * @param codeSource The code source.
	 */
	public CodeSourceFacade(final CodeSource codeSource) {
		this.location = codeSource.getLocation();

		final Certificate[] certificates = codeSource.getCertificates();
		if (null == certificates || 0 == certificates.length) {
			LOGGER.warning(String.format("no certificate found for %s", codeSource));
			this.firstCertificate = null;
			return;
		}

		this.firstCertificate = (X509Certificate)certificates[0];
	}

	/**
	 * Gets the code source location.
	 *
	 * @return The code source location.
	 */
	public URL getLocation() {
		return this.location;
	}

	/**
	 * Gets the first certificate associated with the code source.
	 *
	 * @return The first certificate associated with the code source.
	 */
	public X509Certificate getFirstCertificate() {
		return this.firstCertificate;
	}
}
