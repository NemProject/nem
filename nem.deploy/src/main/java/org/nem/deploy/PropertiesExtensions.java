package org.nem.deploy;

import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 * Helper class for dealing with Java Properties.
 */
public class PropertiesExtensions {
	private static final Logger LOGGER = Logger.getLogger(PropertiesExtensions.class.getName());

	/**
	 * Merges multiple properties objects. In the case of conflict, values in later properties objects take precedence.
	 *
	 * @return The merged properties.
	 */
	public static Properties merge(final Collection<Properties> propBags) {
		final Properties merged = new Properties();
		for (final Properties props : propBags) {
			for (final String name : props.stringPropertyNames()) {
				final String value = props.getProperty(name);
				merged.setProperty(name, value);
			}
		}

		return merged;
	}

	/**
	 * Loads properties from a resource.
	 *
	 * @param clazz The class used to load the resource.
	 * @param name The resource name.
	 * @param isRequired true if the resource is required.
	 * @return The merged properties.
	 */
	public static Properties loadFromResource(final Class<?> clazz, final String name, final boolean isRequired) {
		try (final InputStream inputStream = clazz.getClassLoader().getResourceAsStream(name)) {
			if (null == inputStream) {
				throw new IOException("resource does not exist");
			}

			final Properties properties = new Properties();
			properties.load(inputStream);
			return properties;
		} catch (final IOException ex) {
			// note: only log if something really bad happened because logging might not have been bootstrapped yet!
			if (isRequired) {
				final String message = String.format("unable to load configuration from '%s'", name);
				LOGGER.log(Level.SEVERE, message);
				throw new IllegalArgumentException(message, ex);
			}

			return new Properties();
		}
	}
}
