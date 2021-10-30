package org.nem.nis.time.synchronization.filter;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.NodeAge;
import org.nem.core.test.TimeSyncUtils;
import org.nem.core.time.synchronization.TimeSynchronizationSample;

import java.util.List;

public class ClampingFilterTest {

	@Test
	public void getMaximumToleratedDeviationReturnsToleratedDeviationStartForAgeSmallerThanOrEqualToStartDecayAfterRound() {
		// Arrange:
		final ClampingFilter filter = new ClampingFilter();

		// Assert:
		MatcherAssert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(0)),
				IsEqual.equalTo(FilterConstants.TOLERATED_DEVIATION_START));
		MatcherAssert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND)),
				IsEqual.equalTo(FilterConstants.TOLERATED_DEVIATION_START));
	}

	@Test
	public void getMaximumToleratedDeviationDecaysToToleratedDeviationMinimumAfterStartDecayAfterRound() {
		// Arrange:
		final ClampingFilter filter = new ClampingFilter();

		// Assert:
		// Assuming decay strength 0.3 for the following tests:
		// tolerated = exp(-0.3) * 120 * 60000 = 5333891
		MatcherAssert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 1)),
				IsEqual.equalTo(5333891L));
		// tolerated = exp(-1.5) * 120 * 60000 = 1606537
		MatcherAssert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 5)),
				IsEqual.equalTo(1606537L));
		// tolerated = exp(-3.3) * 120 * 60000 = 265558
		MatcherAssert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 11)),
				IsEqual.equalTo(265558L));
		// tolerated = exp(-4.5) * 120 * 60000 = 79984
		MatcherAssert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 15)),
				IsEqual.equalTo(79984L));
		// exp(-4.8) * 120 * 60000 < TOLERATED_DEVIATION_MINIMUM
		MatcherAssert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 16)),
				IsEqual.equalTo(FilterConstants.TOLERATED_DEVIATION_MINIMUM));
	}

	@Test
	public void filterDoesFilterOutOnlySamplesWithIntolerableTimeOffset() {
		// Arrange:
		final ClampingFilter filter = new ClampingFilter();
		final List<TimeSynchronizationSample> originalSamples = TimeSyncUtils.createTolerableSortedSamples(0, 3);
		originalSamples.addAll(TimeSyncUtils.createIntolerableSamples(5));

		// Act:
		final List<TimeSynchronizationSample> samples = filter.filter(originalSamples, new NodeAge(0));

		// Assert:
		MatcherAssert.assertThat(samples.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(samples, IsEqual.equalTo(TimeSyncUtils.createTolerableSortedSamples(0, 3)));
	}
}
