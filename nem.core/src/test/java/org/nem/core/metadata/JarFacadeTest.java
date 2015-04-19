package org.nem.core.metadata;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.io.*;
import java.net.URL;
import java.util.jar.*;

public class JarFacadeTest {

	//region from url

	@Test
	public void canCreateFacadeAroundWebStartJarFile() throws IOException {
		// Act:
		final URL url = MetaDataTestUtils.createMockUrl("http://path/nem.jar", null);
		final JarFacade facade = new JarFacade(url);

		// Assert:
		Assert.assertThat(facade.getName(), IsEqual.equalTo("nem.jar"));
		Assert.assertThat(facade.isWebStart(), IsEqual.equalTo(true));
		assertFacadeHasDefaultProperties(facade);
	}

	@Test
	public void canCreateFacadeAroundNonWebStartJarFile() throws IOException {
		// Act:
		final URL url = MetaDataTestUtils.createMockUrl("file://path/nem.jar", null);
		final JarFacade facade = new JarFacade(url);

		// Assert:
		Assert.assertThat(facade.getName(), IsEqual.equalTo("nem.jar"));
		Assert.assertThat(facade.isWebStart(), IsEqual.equalTo(false));
		assertFacadeHasDefaultProperties(facade);
	}

	//endregion

	//region manifest processing

	@Test
	public void canCreateFacadeAroundJarStreamWithIoException() throws IOException {
		// Act:
		final URL url = MetaDataTestUtils.createMockUrl("file://path/nem.jar", null);
		final JarFacade facade = new JarFacade(url);

		// Assert:
		Assert.assertThat(facade.getName(), IsEqual.equalTo("nem.jar"));
		Assert.assertThat(facade.isWebStart(), IsEqual.equalTo(false));
		assertFacadeHasDefaultProperties(facade);
	}

	@Test
	public void canCreateFacadeAroundJarStreamWithoutManifest() throws IOException {
		// Arrange:
		final byte[] bytes = MetaDataTestUtils.createJarBytes(null);
		try (final InputStream inputStream = new ByteArrayInputStream(bytes)) {
			// Act:
			final URL url = MetaDataTestUtils.createMockUrl("file://path/nem.jar", inputStream);
			final JarFacade facade = new JarFacade(url);

			// Assert:
			Assert.assertThat(facade.getName(), IsEqual.equalTo("nem.jar"));
			Assert.assertThat(facade.isWebStart(), IsEqual.equalTo(false));
			assertFacadeHasDefaultProperties(facade);
		}
	}

	@Test
	public void canCreateFacadeAroundJarStreamWithManifestWithoutNemVendor() throws IOException {
		// Arrange:
		final Manifest manifest = new Manifest();
		final Attributes attributes = manifest.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		attributes.putValue("Implementation-Vendor", "NEM - New Economy Movement Not!");
		attributes.putValue("Implementation-Version", "test-version");
		attributes.putValue("Implementation-Title", "test-title");

		final byte[] bytes = MetaDataTestUtils.createJarBytes(manifest);
		try (final InputStream inputStream = new ByteArrayInputStream(bytes)) {
			final URL url = MetaDataTestUtils.createMockUrl("file://path/nem.jar", inputStream);
			final JarFacade facade = new JarFacade(url);

			// Assert:
			Assert.assertThat(facade.getName(), IsEqual.equalTo("nem.jar"));
			Assert.assertThat(facade.isWebStart(), IsEqual.equalTo(false));
			assertFacadeHasDefaultProperties(facade);
		}
	}

	@Test
	public void canCreateFacadeAroundJarStreamWithManifestWithNemVendor() throws IOException {
		// Assert:
		assertValidNemVendorName("nem - new ECONOMY Movement");
	}

	@Test
	public void canCreateFacadeAroundJarStreamWithManifestWithCaseInsensitiveNemVendor() throws IOException {
		// Assert:
		assertValidNemVendorName("NEM - New Economy Movement");
	}

	private static void assertValidNemVendorName(final String name) throws IOException {
		// Arrange:
		final Manifest manifest = new Manifest();
		final Attributes attributes = manifest.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		attributes.putValue("Implementation-Vendor", name);
		attributes.putValue("Implementation-Version", "test-version");
		attributes.putValue("Implementation-Title", "test-title");

		final byte[] bytes = MetaDataTestUtils.createJarBytes(manifest);
		try (final InputStream inputStream = new ByteArrayInputStream(bytes)) {
			final URL url = MetaDataTestUtils.createMockUrl("file://path/nem.jar", inputStream);
			final JarFacade facade = new JarFacade(url);

			// Assert:
			Assert.assertThat(facade.getName(), IsEqual.equalTo("nem.jar"));
			Assert.assertThat(facade.isWebStart(), IsEqual.equalTo(false));
			Assert.assertThat(facade.getVersion(), IsEqual.equalTo("test-version"));
			Assert.assertThat(facade.getTitle(), IsEqual.equalTo("test-title"));
		}
	}

	//endregion

	private static void assertFacadeHasDefaultProperties(final JarFacade facade) {
		// Assert:
		Assert.assertThat(facade.getVersion(), IsEqual.equalTo("0.6.0-DEVELOPER BUILD"));
		Assert.assertThat(facade.getTitle(), IsEqual.equalTo("NEM"));
	}
}