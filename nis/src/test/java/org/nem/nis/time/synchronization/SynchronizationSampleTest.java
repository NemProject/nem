package org.nem.nis.time.synchronization;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.primitive.NetworkTimeStamp;
import org.nem.core.node.NodeIdentity;
import org.nem.nis.test.TimeSyncUtils;

import java.util.HashMap;

public class SynchronizationSampleTest {

	private static final KeyPair KEY_PAIR = new KeyPair();

	//region constructor

	@Test
	public void canCreateSynchronizationSample() {
		// Act:
		final SynchronizationSample sample = TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 5, 17, 23, 26);

		// Assert:
		Assert.assertThat(sample.getNode().getIdentity(), IsEqual.equalTo(new NodeIdentity(KEY_PAIR, "node")));
		Assert.assertThat(sample.getLocalTimeStamps(), IsEqual.equalTo(new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(17))));
		Assert.assertThat(sample.getRemoteTimeStamps(), IsEqual.equalTo(new CommunicationTimeStamps(new NetworkTimeStamp(23), new NetworkTimeStamp(26))));
	}

	//endregion

	//region offset calculation

	@Test
	public void timeOffsetIsCalculatedCorrectly() {
		// Arrange:
		final SynchronizationSample sample1 = TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);
		final SynchronizationSample sample2 = TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 8, 12, 45, 45);
		final SynchronizationSample sample3 = TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 37, 43, 15, 13);

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
		final SynchronizationSample sample1 = TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);
		final SynchronizationSample sample2 = TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 8, 12, 45, 45);
		final SynchronizationSample sample3 = TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);

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
		final SynchronizationSample sample = TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);
		final HashMap<String, SynchronizationSample> sampleMap = createTestSynchronizationSampleList();

		// Assert:
		Assert.assertThat(sampleMap.get("default"), IsEqual.equalTo(sample));
		Assert.assertThat(sampleMap.get("diff-identity"), IsNot.not(IsEqual.equalTo(sample)));
		Assert.assertThat(sampleMap.get("diff-local"), IsNot.not(IsEqual.equalTo(sample)));
		Assert.assertThat(sampleMap.get("diff-remote"), IsNot.not(IsEqual.equalTo(sample)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(sample)));
		Assert.assertThat("foo", IsNot.not(IsEqual.equalTo((Object)sample)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final SynchronizationSample sample = TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);
		final HashMap<String, SynchronizationSample> sampleMap = createTestSynchronizationSampleList();

		// Assert:
		Assert.assertThat(sampleMap.get("default").hashCode(), IsEqual.equalTo(sample.hashCode()));
		Assert.assertThat(sampleMap.get("diff-identity").hashCode(), IsNot.not(IsEqual.equalTo(sample.hashCode())));
		Assert.assertThat(sampleMap.get("diff-local-timeStamp").hashCode(), IsNot.not(IsEqual.equalTo(sample.hashCode())));
		Assert.assertThat(sampleMap.get("diff-remote-timeStamp").hashCode(), IsNot.not(IsEqual.equalTo(sample.hashCode())));
	}

	//endregion

	private HashMap<String, SynchronizationSample> createTestSynchronizationSampleList() {
		return new HashMap<String, SynchronizationSample>() {
			{
				this.put("default", TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 5, 17, 25, 23));
				this.put("diff-identity", TimeSyncUtils.createSynchronizationSample(new KeyPair(), 5, 17, 25, 23));
				this.put("diff-local-timeStamp", TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 4, 12, 25, 23));
				this.put("diff-remote-timeStamp", TimeSyncUtils.createSynchronizationSample(KEY_PAIR, 5, 17, 30, 28));
			}
		};
	}
}
