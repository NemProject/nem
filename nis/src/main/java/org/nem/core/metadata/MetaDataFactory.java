package org.nem.core.metadata;

import org.nem.core.time.TimeProvider;

import java.security.CodeSource;

/**
 * Static class that is able to load meta data from various sources.
 */
public class MetaDataFactory {

	/**
	 * Loads ApplicationMetaData given a class and a time provider.
	 *
	 * @param clazz The class.
	 * @param provider The time provider.
	 * @return The application meta data.
	 */
	public static ApplicationMetaData loadApplicationMetaData(final Class clazz, final TimeProvider provider) {
		return loadApplicationMetaData(clazz.getProtectionDomain().getCodeSource(), provider);
	}

	/**
	 * Loads ApplicationMetaData given a CodeSource and a time provider.
	 *
	 * @param codeSource The code source.
	 * @param provider The time provider.
	 * @return The application meta data.
	 */
	public static ApplicationMetaData loadApplicationMetaData(final CodeSource codeSource, final TimeProvider provider) {
		final CodeSourceFacade codeSourceFacade = new CodeSourceFacade(codeSource);
		final JarFacade jarFacade = new JarFacade(codeSourceFacade.getLocation());

		return new ApplicationMetaData(
				jarFacade.getTitle(),
				jarFacade.getVersion(),
				codeSourceFacade.getFirstCertificate(),
				provider);
	}
}
