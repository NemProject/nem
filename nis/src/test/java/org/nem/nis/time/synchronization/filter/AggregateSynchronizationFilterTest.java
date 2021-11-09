package org.nem.nis.time.synchronization.filter;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.primitive.NodeAge;
import org.nem.core.test.TimeSyncUtils;
import org.nem.core.time.synchronization.TimeSynchronizationSample;

import java.util.*;

public class AggregateSynchronizationFilterTest {

	@Test
	public void filterDelegatesToAllFilters() {
		// Arrange:
		final List<SynchronizationFilter> filters = createFilters();
		filters.stream().forEach(f -> Mockito.when(f.filter(Mockito.any(), Mockito.any())).thenReturn(null));
		final AggregateSynchronizationFilter filter = new AggregateSynchronizationFilter(filters);

		// Act:
		filter.filter(null, null);

		// Assert:
		filters.stream().forEach(f -> Mockito.verify(f, Mockito.times(1)).filter(Mockito.any(), Mockito.any()));
	}

	@Test
	public void filtersAreAppliedInGivenOrder() {
		// Arrange:
		final List<SynchronizationFilter> filters = createFilters();
		filters.stream().forEach(f -> Mockito.when(f.filter(Mockito.any(), Mockito.any())).thenReturn(null));
		final AggregateSynchronizationFilter filter = new AggregateSynchronizationFilter(filters);
		final InOrder inOrder = Mockito.inOrder(filters.toArray());

		// Act:
		filter.filter(null, null);

		// Assert:
		filters.stream().forEach(f -> inOrder.verify(f).filter(Mockito.any(), Mockito.any()));
	}

	@Test
	public void filtersAreChained() {
		// Arrange:
		final List<SynchronizationFilter> filters = createFilters();
		final List<TimeSynchronizationSample> originalSamples = TimeSyncUtils.createTolerableSortedSamples(0, 10);
		final List<TimeSynchronizationSample> samples1 = TimeSyncUtils.createTolerableSortedSamples(20, 10);
		final List<TimeSynchronizationSample> samples2 = TimeSyncUtils.createTolerableSortedSamples(40, 10);
		final List<TimeSynchronizationSample> samples3 = TimeSyncUtils.createTolerableSortedSamples(60, 10);
		Mockito.when(filters.get(0).filter(originalSamples, new NodeAge(0))).thenReturn(samples1);
		Mockito.when(filters.get(1).filter(samples1, new NodeAge(0))).thenReturn(samples2);
		Mockito.when(filters.get(2).filter(samples2, new NodeAge(0))).thenReturn(samples3);
		final AggregateSynchronizationFilter filter = new AggregateSynchronizationFilter(filters);

		// Act:
		final List<TimeSynchronizationSample> samples = filter.filter(originalSamples, new NodeAge(0));

		// Assert:
		Mockito.verify(filters.get(0), Mockito.times(1)).filter(originalSamples, new NodeAge(0));
		Mockito.verify(filters.get(1), Mockito.times(1)).filter(samples1, new NodeAge(0));
		Mockito.verify(filters.get(2), Mockito.times(1)).filter(samples2, new NodeAge(0));
		MatcherAssert.assertThat(samples, IsEqual.equalTo(samples3));
	}

	private static List<SynchronizationFilter> createFilters() {
		return Arrays.asList(Mockito.mock(ResponseDelayDetectionFilter.class), Mockito.mock(ClampingFilter.class),
				Mockito.mock(AlphaTrimmedMeanFilter.class));
	}
}
