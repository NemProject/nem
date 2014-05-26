package org.nem.core.metadata;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.core.time.*;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;

public class ApplicationMetaDataTest {

	@Test
	public void canCreateApplicationMetaDataWithoutCertificate() {
		// Arrange:
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(100), new TimeInstant(125));

		// Act:
		final ApplicationMetaData metaData = new ApplicationMetaData("foo", "12.0", null, timeProvider);

		// Assert:
		Assert.assertThat(metaData.getAppName(), IsEqual.equalTo("foo"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("12.0"));
		Assert.assertThat(metaData.getCertificateSigner(), IsNull.nullValue());
		Assert.assertThat(metaData.getStartTime(), IsEqual.equalTo(new TimeInstant(100)));
		Assert.assertThat(metaData.getCurrentTime(), IsEqual.equalTo(new TimeInstant(125)));
	}

	@Test
	public void canCreateApplicationMetaDataWithCertificate() {
		// Arrange:
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(100), new TimeInstant(125));
		final X509Certificate certificate = createMockCertificateWithName("CN=Someone,O=NemSoft");

		// Act:
		final ApplicationMetaData metaData = new ApplicationMetaData("foo", "12.0", certificate, timeProvider);

		// Assert:
		Assert.assertThat(metaData.getAppName(), IsEqual.equalTo("foo"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("12.0"));
		Assert.assertThat(metaData.getCertificateSigner(), IsEqual.equalTo("CN=Someone,O=NemSoft"));
		Assert.assertThat(metaData.getStartTime(), IsEqual.equalTo(new TimeInstant(100)));
		Assert.assertThat(metaData.getCurrentTime(), IsEqual.equalTo(new TimeInstant(125)));
	}

	@Test
	public void canRoundtripApplicationMetaData() {
		// Arrange:
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(100), new TimeInstant(125));
		final X509Certificate certificate = createMockCertificateWithName("CN=Someone,O=NemSoft");
		final ApplicationMetaData originalMetaData = new ApplicationMetaData("foo", "12.0", certificate, timeProvider);

		// Act:
		final ApplicationMetaData metaData = roundtripMetaData(originalMetaData);

		// Assert:
		Assert.assertThat(metaData.getAppName(), IsEqual.equalTo("foo"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("12.0"));
		Assert.assertThat(metaData.getCertificateSigner(), IsEqual.equalTo("CN=Someone,O=NemSoft"));
		Assert.assertThat(metaData.getStartTime(), IsEqual.equalTo(new TimeInstant(100)));
		Assert.assertThat(metaData.getCurrentTime(), IsEqual.equalTo(new TimeInstant(125)));
	}

	@Test
	public void getCurrentTimeAlwaysReturnsMostRecentTime() {
		// Arrange:
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(
				new TimeInstant(100),
				new TimeInstant(125),
				new TimeInstant(136));

		// Act:
		final ApplicationMetaData metaData = new ApplicationMetaData("foo", "12.0", null, timeProvider);

		// Assert:
		Assert.assertThat(metaData.getStartTime(), IsEqual.equalTo(new TimeInstant(100)));
		Assert.assertThat(metaData.getCurrentTime(), IsEqual.equalTo(new TimeInstant(125)));
		Assert.assertThat(metaData.getCurrentTime(), IsEqual.equalTo(new TimeInstant(136)));
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