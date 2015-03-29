package org.nem.core.metadata;

import java.io.*;
import java.net.URL;
import java.util.jar.*;
import java.util.logging.Logger;

/**
 * A facade around a JAR.
 */
public class JarFacade {
	private static final Logger LOGGER = Logger.getLogger(JarFacade.class.getName());

	private static final String NEM_VENDOR = "NEM - New Economy Movement";
	private static final String DEFAULT_TITLE = "NEM";
	private static final String DEFAULT_VERSION = "0.6.0-DEVELOPER BUILD";

	private final String name;
	private final boolean isWebStart;
	private final String title;
	private final String version;

	/**
	 * Creates a new facade.
	 *
	 * @param url The url of the jar file.
	 */
	public JarFacade(final URL url) {

		final String[] urlParts = url.getPath().split("/");
		this.name = urlParts[urlParts.length - 1];
		this.isWebStart = !url.getProtocol().equals("file");

		LOGGER.info(String.format("Analyzing meta data in <%s>", this.name));

		NemAttributes attributes = null;
		try (final InputStream jarStream = url.openStream()) {
			try (final JarInputStream jarInputStream = new JarInputStream(jarStream, true)) {
				attributes = loadAttributes(jarInputStream, this.name);
			}
		} catch (final IOException e) {
			LOGGER.warning(String.format("Analyzing meta data not possible <%s>", e.getMessage()));
		}

		this.title = null == attributes ? DEFAULT_TITLE : attributes.title;
		this.version = null == attributes ? DEFAULT_VERSION : attributes.version;

		LOGGER.info(String.format("Meta data title <%s>, version <%s>", this.title, this.version));
	}

	/**
	 * Gets the name of the JAR file.
	 *
	 * @return The name of the JAR file.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets a value indicating whether or not this is a WebStart JAR file.
	 *
	 * @return true if this is a WebStart JAR file.
	 */
	public boolean isWebStart() {
		return this.isWebStart;
	}

	/**
	 * Gets the title from the JAR manifest.
	 *
	 * @return The title.
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Gets the version from the JAR manifest.
	 *
	 * @return The version.
	 */
	public String getVersion() {
		return this.version;
	}

	private static NemAttributes loadAttributes(final JarInputStream jarInputStream, final String name) {
		final Manifest manifest = jarInputStream.getManifest();
		if (null == manifest) {
			LOGGER.warning(String.format("could not find manifest for %s", name));
			return null;
		}

		final Attributes jarAttributes = manifest.getMainAttributes();
		if (!NEM_VENDOR.equalsIgnoreCase(jarAttributes.getValue("Implementation-Vendor"))) {
			LOGGER.warning(String.format("unexpected implementation vendor for manifest in %s", name));
			return null;
		}

		final NemAttributes nemAttributes = new NemAttributes();
		nemAttributes.version = jarAttributes.getValue("Implementation-Version");
		nemAttributes.title = jarAttributes.getValue("Implementation-Title");
		return nemAttributes;
	}

	private static class NemAttributes {
		public String version;
		public String title;
	}
}
