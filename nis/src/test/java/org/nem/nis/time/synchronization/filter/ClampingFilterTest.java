package org.nem.nis.time.synchronization.filter;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeEndpoint;
import org.nem.nis.time.synchronization.*;

import java.util.*;

public class ClampingFilterTest {

	@Test
	public void getMaximumToleratedDeviationReturnsToleratedDeviationStartForAgeSmallerThanOrEqualToStartDecayAfterRound() {
		// Arrange:
		final ClampingFilter filter = new ClampingFilter();

		// Assert:
		Assert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(0)),
				IsEqual.equalTo(FilterConstants.TOLERATED_DEVIATION_START));
		Assert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND)),
				IsEqual.equalTo(FilterConstants.TOLERATED_DEVIATION_START));
	}

	@Test
	public void getMaximumToleratedDeviationDecaysToToleratedDecayMinimumAfterDecayAfterRound() {
		// Arrange:
		final ClampingFilter filter = new ClampingFilter();

		// Assert:
		assertLongIsWithingRange(
				filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 1)),
				FilterConstants.TOLERATED_DEVIATION_MINIMUM,
				FilterConstants.TOLERATED_DEVIATION_START);
		assertLongIsWithingRange(
				filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 5)),
				FilterConstants.TOLERATED_DEVIATION_MINIMUM,
				FilterConstants.TOLERATED_DEVIATION_START);
		Assert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 10)),
				IsEqual.equalTo(FilterConstants.TOLERATED_DEVIATION_MINIMUM));
	}

	@Test
	public void filterDoesFilterOutOnlySamplesWithIntolerableTimeOffset() {
		// Arrange:
		final ClampingFilter filter = new ClampingFilter();
		final List<SynchronizationSample> originalSamples = createTolerableSamples();
		originalSamples.addAll(createIntolerableSamples());

		// Act:
		final List<SynchronizationSample> samples = filter.filter(originalSamples, new NodeAge(0));

		// Assert:
		Assert.assertThat(samples.size(), IsEqual.equalTo(3));
		Assert.assertThat(samples.contains(createSynchronizationSample(5000)), IsEqual.equalTo(true));
		Assert.assertThat(samples.contains(createSynchronizationSample(6000)), IsEqual.equalTo(true));
		Assert.assertThat(samples.contains(createSynchronizationSample(7000)), IsEqual.equalTo(true));
	}

	private void assertLongIsWithingRange(final long value, final long min, final long max) {
		Assert.assertThat(value > min, IsEqual.equalTo(true));
		Assert.assertThat(value < max, IsEqual.equalTo(true));
	}

	private List<SynchronizationSample> createTolerableSamples() {
		List<SynchronizationSample> samples = new ArrayList<>();
		samples.add(createSynchronizationSample(5000));
		samples.add(createSynchronizationSample(6000));
		samples.add(createSynchronizationSample(7000));

		return samples;
	}

	private List<SynchronizationSample> createIntolerableSamples() {
		List<SynchronizationSample> samples = new ArrayList<>();
		samples.add(createSynchronizationSample(500000));
		samples.add(createSynchronizationSample(600000));
		samples.add(createSynchronizationSample(700000));

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
