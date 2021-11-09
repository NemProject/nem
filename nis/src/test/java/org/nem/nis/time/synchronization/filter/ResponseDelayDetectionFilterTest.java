package org.nem.nis.time.synchronization.filter;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.NodeAge;
import org.nem.core.test.*;
import org.nem.core.time.synchronization.TimeSynchronizationSample;

import java.util.*;

public class ResponseDelayDetectionFilterTest {

	@Test
	public void filterFiltersOutSamplesWithIntolerableDuration() {
		// Arrange:
		final SynchronizationFilter filter = new ResponseDelayDetectionFilter();
		final List<TimeSynchronizationSample> samples = new ArrayList<>();
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM + 1));
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM + 10));
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM + 100));
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM + 1000));

		// Act:
		final List<TimeSynchronizationSample> filteredSamples = filter.filter(samples, new NodeAge(0));

		// Assert:
		MatcherAssert.assertThat(filteredSamples.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void filterDoesNotFilterOutSamplesWithTolerableDuration() {
		// Arrange:
		final SynchronizationFilter filter = new ResponseDelayDetectionFilter();
		final List<TimeSynchronizationSample> samples = new ArrayList<>();
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM - 10));
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM - 1));
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM));

		// Act:
		final List<TimeSynchronizationSample> filteredSamples = filter.filter(samples, new NodeAge(0));

		// Assert:
		MatcherAssert.assertThat(filteredSamples, IsEquivalent.equivalentTo(samples));
	}

	@Test
	public void filterOnlyFiltersOutSamplesWithIntolerableDurationWhenIntolerableAndTolerableDurationsAreMixed() {
		// Arrange:
		final SynchronizationFilter filter = new ResponseDelayDetectionFilter();
		final List<TimeSynchronizationSample> samples = new ArrayList<>();
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM + 1));
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM - 1));
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM + 10));
		samples.add(TimeSyncUtils.createTimeSynchronizationSampleWithDuration(FilterConstants.TOLERATED_DURATION_MAXIMUM));

		// Act:
		final List<TimeSynchronizationSample> filteredSamples = filter.filter(samples, new NodeAge(0));

		// Assert:
		MatcherAssert.assertThat(filteredSamples, IsEquivalent.equivalentTo(samples.get(1), samples.get(3)));
	}
}
