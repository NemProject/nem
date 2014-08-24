package org.nem.nis.time.synchronization;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.NetworkTimeStamp;
import org.nem.core.node.NodeEndpoint;

import java.util.*;

public class SynchronizationSampleTest {

	//region constructor

	@Test
	public void canCreateSynchronizationSample() {
		// Act:
		final SynchronizationSample sample = createSynchronizationSample(5, 17, 23, 26);

		// Assert:
		Assert.assertThat(sample.getEndpoint(), IsEqual.equalTo(new NodeEndpoint("ftp", "10.8.8.2", 12)));
		Assert.assertThat(sample.getLocalTimeStamps(), IsEqual.equalTo(new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(17))));
		Assert.assertThat(sample.getRemoteTimeStamps(), IsEqual.equalTo(new CommunicationTimeStamps(new NetworkTimeStamp(23), new NetworkTimeStamp(26))));
	}

	//endregion

	//region offset calculation

	@Test
	public void timeOffsetIsCalculatedCorrectly() {
		// Arrange:
		final SynchronizationSample sample1 = createSynchronizationSample(5, 17, 25, 23);
		final SynchronizationSample sample2 = createSynchronizationSample(8, 12, 45, 45);
		final SynchronizationSample sample3 = createSynchronizationSample(37, 43, 15, 13);

		// Assert:
		Assert.assertThat(sample1.getTimeOffsetToRemote(), IsEqual.equalTo(13L));
		Assert.assertThat(sample2.getTimeOffsetToRemote(), IsEqual.equalTo(35L));
		Assert.assertThat(sample3.getTimeOffsetToRemote(), IsEqual.equalTo(-26L));
	}

	//endregion

	//region compareTo

	@Test
	public void canCompareSynchronizationSamples() {
		// Arrange:
		final SynchronizationSample sample1 = createSynchronizationSample(5, 17, 25, 23);
		final SynchronizationSample sample2 = createSynchronizationSample(8, 12, 45, 45);
		final SynchronizationSample sample3 = createSynchronizationSample(5, 17, 25, 23);

		// Assert:
		Assert.assertThat(sample1.compareTo(sample2), IsEqual.equalTo(-1));
		Assert.assertThat(sample2.compareTo(sample1), IsEqual.equalTo(1));
		Assert.assertThat(sample1.compareTo(sample3), IsEqual.equalTo(0));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final List<SynchronizationSample> sampleList = createTestSynchronizationSampleList();

		// Assert:
		Assert.assertThat(sampleList.get(0), IsEqual.equalTo(sampleList.get(1)));
		Assert.assertThat(sampleList.get(0), IsNot.not(IsEqual.equalTo(sampleList.get(2))));
		Assert.assertThat(sampleList.get(0), IsNot.not(IsEqual.equalTo(sampleList.get(3))));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final List<SynchronizationSample> sampleList = createTestSynchronizationSampleList();

		// Assert:
		Assert.assertThat(sampleList.get(0).hashCode(), IsEqual.equalTo(sampleList.get(1).hashCode()));
		Assert.assertThat(sampleList.get(0).hashCode(), IsNot.not(IsEqual.equalTo(sampleList.get(2).hashCode())));
		Assert.assertThat(sampleList.get(0).hashCode(), IsNot.not(IsEqual.equalTo(sampleList.get(3).hashCode())));
	}

	//endregion

	private SynchronizationSample createSynchronizationSample(
			final long localSendTimeStamp,
			final long localReceiveTimeStamp,
			final long remoteSendTimeStamp,
			final long remoteReceiveTimeStamp) {
		return new SynchronizationSample(
			new NodeEndpoint("ftp", "10.8.8.2", 12),
			new CommunicationTimeStamps(new NetworkTimeStamp(localSendTimeStamp), new NetworkTimeStamp(localReceiveTimeStamp)),
			new CommunicationTimeStamps(new NetworkTimeStamp(remoteSendTimeStamp), new NetworkTimeStamp(remoteReceiveTimeStamp)));
	}

	private List<SynchronizationSample> createTestSynchronizationSampleList() {
		return Arrays.asList(
				createSynchronizationSample(5, 17, 25, 23),
				createSynchronizationSample(5, 17, 25, 23),
				createSynchronizationSample(4, 12, 25, 23),
				createSynchronizationSample(5, 17, 30, 28));
	}
}
