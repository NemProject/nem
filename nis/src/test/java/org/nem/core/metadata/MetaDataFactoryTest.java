package org.nem.core.metadata;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.time.*;

import java.io.*;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.*;
import java.util.jar.*;

public class MetaDataFactoryTest {

	@Test
	public void canLoadApplicationMetaDataFromCodeSource() throws Exception {
		// Arrange:
		final Manifest manifest = new Manifest();
		final Attributes attributes = manifest.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		attributes.putValue("Implementation-Vendor", "NEM - New Economy Movement");
		attributes.putValue("Implementation-Version", "test-version");
		attributes.putValue("Implementation-Title", "test-title");

		final byte[] bytes = MetaDataTestUtils.createJarBytes(manifest);
		try (final InputStream inputStream = new ByteArrayInputStream(bytes)) {
			final URL url = MetaDataTestUtils.createMockUrl("file://path/nem.jar", inputStream);

			final X509Certificate certificate = MetaDataTestUtils.createMockCertificateWithName("CN=Someone,O=NemSoft");
			final CodeSource codeSource = new CodeSource(url, new Certificate[] { certificate });

			final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
			Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(11), new TimeInstant(14));

			// Act:
			final ApplicationMetaData metaData = MetaDataFactory.loadApplicationMetaData(codeSource, timeProvider);

			// Assert:
			Assert.assertThat(metaData.getAppName(), IsEqual.equalTo("test-title"));
			Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("test-version"));
			Assert.assertThat(metaData.getCertificateSigner(), IsEqual.equalTo("CN=Someone,O=NemSoft"));
			Assert.assertThat(metaData.getStartTime(), IsEqual.equalTo(new TimeInstant(11)));
			Assert.assertThat(metaData.getCurrentTime(), IsEqual.equalTo(new TimeInstant(14)));
		}
	}
}