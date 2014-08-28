package org.nem.nis.time.synchronization.filter;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.test.TimeSyncUtils;
import org.nem.nis.time.synchronization.SynchronizationSample;

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
		assertLongIsWithingRange(
				filter.getMaximumToleratedDeviation(new NodeAge(FilterConstants.START_DECAY_AFTER_ROUND + 1)),
				FilterConstants.TOLERATED_DEVIATION_MINIMUM,
				FilterConstants.TOLERATED_DEVIATION_START); //TODO-CR: J-B i think checking the range [MIN, START] is too big or at least have one test that calculates a ~exact value
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
		final List<SynchronizationSample> originalSamples = TimeSyncUtils.createTolerableSamples(0, 3, true);
		originalSamples.addAll(TimeSyncUtils.createIntolerableSamples(5));

		// Act:
		final List<SynchronizationSample> samples = filter.filter(originalSamples, new NodeAge(0));

		// Assert:
		Assert.assertThat(samples.size(), IsEqual.equalTo(3));
		Assert.assertThat(samples, IsEqual.equalTo(TimeSyncUtils.createTolerableSamples(0, 3, true)));
	}

	private void assertLongIsWithingRange(final long value, final long min, final long max) {
		Assert.assertThat(value > min, IsEqual.equalTo(true));
		Assert.assertThat(value < max, IsEqual.equalTo(true));
	}
}
