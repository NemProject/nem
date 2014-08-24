package org.nem.nis.time.synchronization.filter;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.NetworkTimeStamp;
import org.nem.core.node.NodeEndpoint;
import org.nem.nis.time.synchronization.*;

import java.util.*;

public class AlphaTrimmedMeanFilterTest {

	@Test
	public void filterDiscardsSamplesAtBothEnds() {
		assertFilterWorksAsExpected(createSortedSamples());
	}

	@Test
	public void filterCanHandleUnsortedList() {
		assertFilterWorksAsExpected(createUnsortedSamples());
	}

	private void assertFilterWorksAsExpected(List<SynchronizationSample> originalSamples) {
		// Arrange:
		final AlphaTrimmedMeanFilter filter = new AlphaTrimmedMeanFilter();

		// Act:
		final List<SynchronizationSample> samples = filter.filter(originalSamples, null);

		// Assert:
		int value = (int)(originalSamples.size() * FilterConstants.ALPHA / 2);
		Assert.assertThat(samples.size(), IsEqual.equalTo(originalSamples.size() - 2 * value));
		for (SynchronizationSample sample : samples) {
			Assert.assertThat(sample, IsEqual.equalTo(createSynchronizationSample(1000 * value++)));
		}
	}

	private List<SynchronizationSample> createSortedSamples() {
		List<SynchronizationSample> samples = new ArrayList<>();
		for (int i=0; i<20; i++) {
			samples.add(createSynchronizationSample(1000 * i));
		}

		return samples;
	}

	private List<SynchronizationSample> createUnsortedSamples() {
		List<SynchronizationSample> samples = new ArrayList<>();
		final long[] offsets = { 3, 4, 5, 15, 16, 17, 18, 19, 0, 1,
								 2, 8, 9, 10, 12, 11, 14, 13, 6, 7 };
		for (int i=0; i<20; i++) {
			samples.add(createSynchronizationSample(1000 * offsets[i]));
		}

		return samples;
	}

	// TODO: 20140824 BR: Better mock the SynchronizationSample class?
	private SynchronizationSample createSynchronizationSample(long timeOffset) {
		return new SynchronizationSample(
				new NodeEndpoint("ftp", "10.8.8.2", 12),
				new CommunicationTimeStamps(new NetworkTimeStamp(100000), new NetworkTimeStamp(100010)),
				new CommunicationTimeStamps(new NetworkTimeStamp(100005 + timeOffset), new NetworkTimeStamp(100005 + timeOffset)));
	}
}
