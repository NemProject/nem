package org.nem.core.time.synchronization;

import java.util.HashMap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.node.NodeIdentity;
import org.nem.core.test.TimeSyncUtils;
import org.nem.core.time.NetworkTimeStamp;

public class TimeSynchronizationSampleTest {

	private static final KeyPair KEY_PAIR = new KeyPair();

	// region constructor

	@Test
	public void canCreateTimeSynchronizationSample() {
		// Act:
		final TimeSynchronizationSample sample = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 5, 17, 23, 26);

		// Assert:
		MatcherAssert.assertThat(sample.getNode().getIdentity(), IsEqual.equalTo(new NodeIdentity(KEY_PAIR, "node")));
		MatcherAssert.assertThat(sample.getLocalTimeStamps(),
				IsEqual.equalTo(new CommunicationTimeStamps(new NetworkTimeStamp(5), new NetworkTimeStamp(17))));
		MatcherAssert.assertThat(sample.getRemoteTimeStamps(),
				IsEqual.equalTo(new CommunicationTimeStamps(new NetworkTimeStamp(23), new NetworkTimeStamp(26))));
	}

	// endregion

	// region duration calculation

	@Test
	public void durationIsCalculatedCorrectly() {
		// Arrange:
		final TimeSynchronizationSample sample1 = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);
		final TimeSynchronizationSample sample2 = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 0, 31, 45, 45);
		final TimeSynchronizationSample sample3 = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 30, 30, 15, 13);

		// Assert:
		MatcherAssert.assertThat(sample1.getDuration(), IsEqual.equalTo(12L));
		MatcherAssert.assertThat(sample2.getDuration(), IsEqual.equalTo(31L));
		MatcherAssert.assertThat(sample3.getDuration(), IsEqual.equalTo(0L));
	}

	// endregion

	// region offset calculation

	@Test
	public void timeOffsetIsCalculatedCorrectly() {
		// Arrange:
		final TimeSynchronizationSample sample1 = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);
		final TimeSynchronizationSample sample2 = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 8, 12, 45, 45);
		final TimeSynchronizationSample sample3 = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 37, 43, 15, 13);

		// Assert:
		MatcherAssert.assertThat(sample1.getTimeOffsetToRemote(), IsEqual.equalTo(13L));
		MatcherAssert.assertThat(sample2.getTimeOffsetToRemote(), IsEqual.equalTo(35L));
		MatcherAssert.assertThat(sample3.getTimeOffsetToRemote(), IsEqual.equalTo(-26L));
	}

	// endregion

	// region compareTo

	@Test
	public void canCompareTimeSynchronizationSamples() {
		// Arrange:
		final TimeSynchronizationSample sample1 = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);
		final TimeSynchronizationSample sample2 = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 8, 12, 45, 45);
		final TimeSynchronizationSample sample3 = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);

		// Assert:
		MatcherAssert.assertThat(sample1.compareTo(sample2), IsEqual.equalTo(-1));
		MatcherAssert.assertThat(sample2.compareTo(sample1), IsEqual.equalTo(1));
		MatcherAssert.assertThat(sample1.compareTo(sample3), IsEqual.equalTo(0));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final TimeSynchronizationSample sample = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);
		final HashMap<String, TimeSynchronizationSample> sampleMap = this.createTestTimeSynchronizationSampleList();

		// Assert:
		MatcherAssert.assertThat(sampleMap.get("default"), IsEqual.equalTo(sample));
		MatcherAssert.assertThat(sampleMap.get("diff-identity"), IsNot.not(IsEqual.equalTo(sample)));
		MatcherAssert.assertThat(sampleMap.get("diff-local"), IsNot.not(IsEqual.equalTo(sample)));
		MatcherAssert.assertThat(sampleMap.get("diff-remote"), IsNot.not(IsEqual.equalTo(sample)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(sample)));
		MatcherAssert.assertThat("foo", IsNot.not(IsEqual.equalTo((Object) sample)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final TimeSynchronizationSample sample = TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 5, 17, 25, 23);
		final HashMap<String, TimeSynchronizationSample> sampleMap = this.createTestTimeSynchronizationSampleList();

		// Assert:
		MatcherAssert.assertThat(sampleMap.get("default").hashCode(), IsEqual.equalTo(sample.hashCode()));
		MatcherAssert.assertThat(sampleMap.get("diff-identity").hashCode(), IsNot.not(IsEqual.equalTo(sample.hashCode())));
		MatcherAssert.assertThat(sampleMap.get("diff-local-timeStamp").hashCode(), IsNot.not(IsEqual.equalTo(sample.hashCode())));
		MatcherAssert.assertThat(sampleMap.get("diff-remote-timeStamp").hashCode(), IsNot.not(IsEqual.equalTo(sample.hashCode())));
	}

	// endregion
	@SuppressWarnings("serial")
	private HashMap<String, TimeSynchronizationSample> createTestTimeSynchronizationSampleList() {
		return new HashMap<String, TimeSynchronizationSample>() {
			{
				this.put("default", TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 5, 17, 25, 23));
				this.put("diff-identity", TimeSyncUtils.createTimeSynchronizationSample(new KeyPair(), 5, 17, 25, 23));
				this.put("diff-local-timeStamp", TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 4, 12, 25, 23));
				this.put("diff-remote-timeStamp", TimeSyncUtils.createTimeSynchronizationSample(KEY_PAIR, 5, 17, 30, 28));
			}
		};
	}
}
