package org.nem.core.metadata;

import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeProvider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

/**
 * Static class that is able to load application meta data from various sources.
 */
public class MetaDataReader {
	private static final Logger LOGGER = Logger.getLogger(MetaDataReader.class.getName());

	// TODO: still need to add tests and clean this up somewhat
	public static ApplicationMetaData extractMetaData(Class clazz) {
		String version = null;
		String appName = null;
		X509Certificate nemCertificate = null;
		// TODO: timeProvider should be passed in ... not sure if it should be owned by CommonStarter or NisMain
		final TimeProvider timeProvider = new SystemTimeProvider();

		CodeSource cs = clazz.getProtectionDomain().getCodeSource();
		URL jarURL = cs.getLocation();

		LOGGER.info(String.format("Code Source for <%s>: <%s>", clazz.getTypeName(), jarURL.toExternalForm()));

		if (jarURL.getPath().endsWith(".jar")) {
			File file;
			try {
				file = new File(jarURL.toURI());
				try (JarFile jarFile = new JarFile(file, true)) {
					Manifest manifest = jarFile.getManifest();

					Attributes mainAttribs = manifest.getMainAttributes();
					if ("NEM - New Economy Movement".equalsIgnoreCase(mainAttribs.getValue("Implementation-Vendor"))) {
						version = mainAttribs.getValue("Implementation-Version");
						appName = mainAttribs.getValue("Implementation-Title");
					}

					Certificate[] certs = cs.getCertificates();
					if (certs.length > 0) {
						nemCertificate = (X509Certificate) certs[0];
						LOGGER.info(String.format("Certificate found: <%s>", nemCertificate.getIssuerX500Principal().getName()));
					}

					return new ApplicationMetaData(appName, version, nemCertificate, timeProvider);
				} catch (IOException e) {
					// Just log
					LOGGER.info(String.format("could not read jar file <%s>", e.getMessage()));
				}
			} catch (URISyntaxException e1) {
				// Just log
				LOGGER.info(String.format("could not read jar file <%s>", e1.getMessage()));
			}
		}

		return new ApplicationMetaData("NEM", "DEVELOPER BUILD", null, timeProvider);
	}
}
