package org.nem.core.metadata;

import java.security.cert.X509Certificate;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.core.time.*;

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
		final X509Certificate certificate = MetaDataTestUtils.createMockCertificateWithName("CN=Someone,O=NemSoft");

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
		final X509Certificate certificate = MetaDataTestUtils.createMockCertificateWithName("CN=Someone,O=NemSoft");
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

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(
				new TimeInstant(100),
				new TimeInstant(100),
				new TimeInstant(100),
				new TimeInstant(125));

		// Act:
		final ApplicationMetaData metaData1 = new ApplicationMetaData("foo", "12.0", null, timeProvider);
		final ApplicationMetaData metaData2 = new ApplicationMetaData("bar", "12.0", null, timeProvider);
		final ApplicationMetaData metaData3 = new ApplicationMetaData("foo", "13.0", null, timeProvider);
		final ApplicationMetaData metaData4 = new ApplicationMetaData("foo", "12.0", null, timeProvider);

		// Assert:
		Assert.assertThat(metaData1, IsEqual.equalTo(metaData4));
		Assert.assertThat(metaData1, IsNot.not(IsEqual.equalTo(metaData2)));
		Assert.assertThat(metaData1, IsNot.not(IsEqual.equalTo(metaData3)));

		// TODO-CR 20140809 - missing comparison with metaData4
		// TODO-CR 20140809 - also, should consider testing comparison with a different certificate
		// TODO-CR 20140809 - also, a good idea to test inequality against null and an object of a different type for completeness
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(
				new TimeInstant(100),
				new TimeInstant(100),
				new TimeInstant(125));

		// Act:
		final ApplicationMetaData metaData1 = new ApplicationMetaData("foo", "12.0", null, timeProvider);
		final ApplicationMetaData metaData2 = new ApplicationMetaData("bar", "12.0", null, timeProvider);
		final ApplicationMetaData metaData3 = new ApplicationMetaData("foo", "13.0", null, timeProvider);
		final ApplicationMetaData metaData4 = new ApplicationMetaData("foo", "12.0", null, timeProvider);

		// Assert:
		Assert.assertThat(metaData1.hashCode(), IsEqual.equalTo(metaData4.hashCode()));
		Assert.assertThat(metaData1.hashCode(), IsNot.not(IsEqual.equalTo(metaData2.hashCode())));
		Assert.assertThat(metaData1.hashCode(), IsNot.not(IsEqual.equalTo(metaData3.hashCode())));

		// TODO-CR 20140809 - same comments as equals
	}

	//endregion

	private static ApplicationMetaData roundtripMetaData(final ApplicationMetaData metaData) {
		return new ApplicationMetaData(Utils.roundtripSerializableEntity(metaData, null));
	}
}