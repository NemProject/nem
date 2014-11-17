package org.nem.core.i18n;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A custom resource bundle control that supports loading UTF8 resources from properties files.
 * See http://stackoverflow.com/a/4660195/1330640.
 */
public class UTF8ResourceBundleControl extends ResourceBundle.Control {
	@Override
	public ResourceBundle newBundle(
			final String baseName,
			final Locale locale,
			final String format,
			final ClassLoader loader,
			final boolean reload) throws IllegalAccessException, InstantiationException, IOException {
		// The below is a copy of the default implementation.
		final String bundleName = this.toBundleName(baseName, locale);
		final String resourceName = this.toResourceName(bundleName, "properties");
		ResourceBundle bundle = null;
		InputStream stream = null;
		if (reload) {
			final URL url = loader.getResource(resourceName);
			if (url != null) {
				final URLConnection connection = url.openConnection();
				if (connection != null) {
					connection.setUseCaches(false);
					stream = connection.getInputStream();
				}
			}
		} else {
			stream = loader.getResourceAsStream(resourceName);
		}
		if (stream != null) {
			try {
				// Only this line is changed to make it to read properties files as UTF-8.
				bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
			} finally {
				stream.close();
			}
		}
		return bundle;
	}
}
