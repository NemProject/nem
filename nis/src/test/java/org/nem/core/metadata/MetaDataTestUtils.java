package org.nem.core.metadata;

import org.mockito.Mockito;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.jar.*;

/**
 * Static class containing helper functions for meta data tests.
 */
public class MetaDataTestUtils {

	/**
	 * Creates a new 509 certificate with the specified name.
	 *
	 * @param name The desired name.
	 * @return The certificate.
	 */
	public static X509Certificate createMockCertificateWithName(final String name) {
		final X509Certificate certificate = Mockito.mock(X509Certificate.class);
		final X500Principal principal = new X500Principal(name);
		Mockito.when(certificate.getIssuerX500Principal()).thenReturn(principal);
		return certificate;
	}

	/**
	 * Creates a mock url.
	 *
	 * @param spec The URL specification.
	 * @param inputStream The JAR input stream.
	 * @return The url.
	 * @throws IOException If an I/O exception occurred.
	 */
	public static URL createMockUrl(final String spec, final InputStream inputStream) throws IOException {
		final URLStreamHandler urlStreamHandler = new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(final URL url) throws IOException {
				if (null == inputStream) {
					throw new IOException();
				}

				final URLConnection urlConnection = Mockito.mock(URLConnection.class);
				Mockito.when(urlConnection.getInputStream()).thenReturn(inputStream);
				return urlConnection;
			}
		};

		final URL templateUrl = new URL(spec);
		return new URL(
				templateUrl.getProtocol(),
				templateUrl.getHost(),
				templateUrl.getPort(),
				templateUrl.getFile(),
				urlStreamHandler);
	}

	/**
	 * Serializes a JAR file with the specified manifest.
	 *
	 * @param manifest The manifest.
	 * @return The serialized JAR file.
	 * @throws IOException If an I/O exception occurred.
	 */
	public static byte[] createJarBytes(final Manifest manifest) throws IOException {
		try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			try (final JarOutputStream outputStream = null == manifest
					? new JarOutputStream(stream)
					: new JarOutputStream(stream, manifest)) {
				outputStream.flush();
			}

			return stream.toByteArray();
		}
	}
}
