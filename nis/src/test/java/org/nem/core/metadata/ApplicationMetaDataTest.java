package org.nem.core.metadata;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;

public class ApplicationMetaDataTest {

	@Test
	public void canCreateApplicationMetaDataWithoutCertificate() {
		// Act:
		final ApplicationMetaData metaData = new ApplicationMetaData("foo", "12.0", null, 11);

		// Assert:
		Assert.assertThat(metaData.getAppName(), IsEqual.equalTo("foo"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("12.0"));
		Assert.assertThat(metaData.getCertificateSigner(), IsNull.nullValue());
		Assert.assertThat(metaData.getStartTime(), IsEqual.equalTo(11L));
	}

	@Test
	public void canCreateApplicationMetaDataWithCertificate() {
		// Act:
		final X509Certificate certificate = createMockCertificateWithName("CN=Someone,O=NemSoft");
		final ApplicationMetaData metaData = new ApplicationMetaData("foo", "12.0", certificate, 11);

		// Assert:
		Assert.assertThat(metaData.getAppName(), IsEqual.equalTo("foo"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("12.0"));
		Assert.assertThat(metaData.getCertificateSigner(), IsEqual.equalTo("CN=Someone,O=NemSoft"));
		Assert.assertThat(metaData.getStartTime(), IsEqual.equalTo(11L));
	}

	@Test
	public void canRoundtripApplicationMetaData() {
		// Act:
		final X509Certificate certificate = createMockCertificateWithName("CN=Someone,O=NemSoft");
		final ApplicationMetaData metaData = roundtripMetaData(new ApplicationMetaData("foo", "12.0", certificate, 11));

		// Assert:
		Assert.assertThat(metaData.getAppName(), IsEqual.equalTo("foo"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("12.0"));
		Assert.assertThat(metaData.getCertificateSigner(), IsEqual.equalTo("CN=Someone,O=NemSoft"));
		Assert.assertThat(metaData.getStartTime(), IsEqual.equalTo(11L));
	}

	private static ApplicationMetaData roundtripMetaData(final ApplicationMetaData metaData) {
		return new ApplicationMetaData(Utils.roundtripSerializableEntity(metaData, null));
	}

	private static X509Certificate createMockCertificateWithName(final String name) {
		final X509Certificate certificate = Mockito.mock(X509Certificate.class);
		final X500Principal principal = new X500Principal(name);
		Mockito.when(certificate.getIssuerX500Principal()).thenReturn(principal);
		return certificate;
	}
}