package org.nem.nis.time.synchronization.filter;

import org.junit.Test;
import org.mockito.*;

import java.util.Arrays;

public class AggregateSynchronizationFilterTest {

	@Test
	public void filterDelegatesToAllFilters() {
		// Arrange:
		final ClampingFilter filter1 = Mockito.mock(ClampingFilter.class);
		final AlphaTrimmedMeanFilter filter2 = Mockito.mock(AlphaTrimmedMeanFilter.class);
		Mockito.when(filter1.filter(Mockito.any(), Mockito.any())).thenReturn(null);
		Mockito.when(filter1.filter(Mockito.any(), Mockito.any())).thenReturn(null);
		final AggregateSynchronizationFilter filter = new AggregateSynchronizationFilter(
				Arrays.asList(filter1, filter2));

		// Act:
		filter.filter(null, null);

		// Assert:
		Mockito.verify(filter1, Mockito.times(1)).filter(Mockito.any(), Mockito.any());
		Mockito.verify(filter2, Mockito.times(1)).filter(Mockito.any(), Mockito.any());
	}

	@Test
	public void filtersAreAppliedInGivenOrder() {
		// Arrange:
		final ClampingFilter filter1 = Mockito.mock(ClampingFilter.class);
		final AlphaTrimmedMeanFilter filter2 = Mockito.mock(AlphaTrimmedMeanFilter.class);
		Mockito.when(filter1.filter(Mockito.any(), Mockito.any())).thenReturn(null);
		Mockito.when(filter1.filter(Mockito.any(), Mockito.any())).thenReturn(null);
		InOrder inOrder = Mockito.inOrder(filter1, filter2);
		final AggregateSynchronizationFilter filter = new AggregateSynchronizationFilter(
				Arrays.asList(filter1, filter2));

		// Act:
		filter.filter(null, null);

		// Assert:
		inOrder.verify(filter1).filter(Mockito.any(), Mockito.any());
		inOrder.verify(filter2).filter(Mockito.any(), Mockito.any());
	}
}
