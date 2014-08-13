package org.nem.core.metadata;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.core.time.*;

import java.security.cert.X509Certificate;
import java.util.*;

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

	private static Map<String, ApplicationMetaData> createApplicationMetaDataForEqualityTests(final X509Certificate certificate) {
		final X509Certificate otherCertificate = MetaDataTestUtils.createMockCertificateWithName("CN=SomeoneElse,O=NemSoft");
		return new HashMap<String, ApplicationMetaData>() {
			{
				this.put("default", new ApplicationMetaData("foo", "12.0", certificate, createTimeProvider(17)));
				this.put("diff-name", new ApplicationMetaData("bar", "12.0", certificate, createTimeProvider(17)));
				this.put("diff-version", new ApplicationMetaData("foo", "11.0", certificate, createTimeProvider(17)));
				this.put("diff-cert", new ApplicationMetaData("foo", "12.0", otherCertificate, createTimeProvider(17)));
				this.put("diff-null-cert", new ApplicationMetaData("foo", "12.0", null, createTimeProvider(17)));
				this.put("diff-time", new ApplicationMetaData("foo", "12.0", certificate, createTimeProvider(22))); // not significant
			}
		};
	}

	private static TimeProvider createTimeProvider(final int time) {
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(time));
		return timeProvider;
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final X509Certificate certificate = MetaDataTestUtils.createMockCertificateWithName("CN=Someone,O=NemSoft");
		final ApplicationMetaData metaData = new ApplicationMetaData("foo", "12.0", certificate, createTimeProvider(17));
		final Map<String, ApplicationMetaData> infoMap = createApplicationMetaDataForEqualityTests(certificate);

		// Assert:
		Assert.assertThat(infoMap.get("default"), IsEqual.equalTo(metaData));
		Assert.assertThat(infoMap.get("diff-name"), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(infoMap.get("diff-version"), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(infoMap.get("diff-cert"), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(infoMap.get("diff-null-cert"), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(infoMap.get("diff-time"), IsEqual.equalTo(metaData));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat("foo", IsNot.not(IsEqual.equalTo((Object)metaData)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final X509Certificate certificate = MetaDataTestUtils.createMockCertificateWithName("CN=Someone,O=NemSoft");
		final ApplicationMetaData metaData = new ApplicationMetaData("foo", "12.0", certificate, createTimeProvider(17));
		final int hashCode = metaData.hashCode();
		final Map<String, ApplicationMetaData> infoMap = createApplicationMetaDataForEqualityTests(certificate);

		// Assert:
		Assert.assertThat(infoMap.get("default").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(infoMap.get("diff-name").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("diff-version").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("diff-cert").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("diff-null-cert").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("diff-time").hashCode(), IsEqual.equalTo(hashCode));
	}

	//endregion

	private static ApplicationMetaData roundtripMetaData(final ApplicationMetaData metaData) {
		return new ApplicationMetaData(Utils.roundtripSerializableEntity(metaData, null));
	}
}