package org.nem.core.metadata;

import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

/**
 * Static class that is able to load application meta data from various sources.
 */
public class MetaDataReader {
	private static final Logger LOGGER = Logger.getLogger(MetaDataReader.class.getName());

	// TODO: still need to add tests and clean this up somewhat
	public static ApplicationMetaData extractMetaData(Class clazz) {
		// TODO: timeProvider should be passed in ... not sure if it should be
		// owned by CommonStarter or NisMain
		final TimeProvider timeProvider = new SystemTimeProvider();
		ApplicationMetaData result = null;

		CodeSource cs = clazz.getProtectionDomain().getCodeSource();
		URL jarURL = cs.getLocation();

		String[] segments = jarURL.getPath().split("/");
		String jarName = segments[segments.length - 1];

		LOGGER.info(String.format("Code Source for <%s>: <%s>", clazz.getTypeName(), jarURL.toExternalForm()));
		LOGGER.info(String.format("Analysing meta data in <%s>", jarName));

		if (jarURL.getProtocol().equals("file")) {
			// Not JavaWebStart
			try (InputStream is = jarURL.openStream()) {
				result = extractMetaDataFromStream(is, cs, timeProvider);
			} catch (IOException e) {
				// 
				LOGGER.info(String.format("Analysing meta data not possible <%s>", e.getMessage()));
			}
		} else {
			// JavaWebStart
			try (InputStream is = jarURL.openStream()) {
				result = extractMetaDataFromStream(is, cs, timeProvider);
			} catch (IOException e) {
				// 
				LOGGER.info(String.format("Analysing meta data not possible <%s>", e.getMessage()));
			}
		}
		
		if(result == null) {
			result = new ApplicationMetaData("NEM", "DEVELOPER BUILD", null, timeProvider);
		}

		return result;
	}

	static protected ApplicationMetaData extractMetaDataFromStream(InputStream is, CodeSource cs, TimeProvider timeProvider) {
		String version = null;
		String appName = null;
		X509Certificate nemCertificate = null;
		ApplicationMetaData result = null;

		try (JarInputStream jarInputStream = new JarInputStream(is, true)) {
			Manifest manifest = jarInputStream.getManifest();
			
			//
			if(manifest == null) {
				LOGGER.info("No manifest file found.");
				return null;
			}

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

			result = new ApplicationMetaData(appName, version, nemCertificate, timeProvider);
		} catch (IOException e) {
			// Just log
			LOGGER.info(String.format("could not read jar file <%s>", e.getMessage()));
		}

		return result;
	}
}
