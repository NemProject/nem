package org.nem.nis.time.synchronization.filter;

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
		Assert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(0)),
				IsEqual.equalTo(FilterConstants.TOLERATED_DEVIATION_START));
		Assert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND)),
				IsEqual.equalTo(FilterConstants.TOLERATED_DEVIATION_START));
	}

	@Test
	public void getMaximumToleratedDeviationDecaysToToleratedDeviationMinimumAfterStartDecayAfterRound() {
		// Arrange:
		final ClampingFilter filter = new ClampingFilter();

		// Assert:
		// Assuming decay strength 0.3 for the following tests:
		// tolerated = exp(-0.3) * 30 * 60000 = 1333472
		Assert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 1)), IsEqual.equalTo(1333472L));
		// tolerated = exp(-1.5) * 30 * 60000 = 401634
		Assert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 5)), IsEqual.equalTo(401634L));
		// tolerated = exp(-3.3) * 30 * 60000 = 66389
		Assert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 11)), IsEqual.equalTo(66389L));
		// exp(-3.6) * 300000 < TOLERATED_DEVIATION_MINIMUM
		Assert.assertThat(filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 12)),
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
		Assert.assertThat(samples.size(), IsEqual.equalTo(3));
		Assert.assertThat(samples, IsEqual.equalTo(TimeSyncUtils.createTolerableSortedSamples(0, 3)));
	}
}
