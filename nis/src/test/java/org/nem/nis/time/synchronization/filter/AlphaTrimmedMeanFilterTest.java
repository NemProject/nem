package org.nem.nis.time.synchronization.filter;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.TimeSyncUtils;
import org.nem.core.time.synchronization.TimeSynchronizationSample;

import java.util.List;

public class AlphaTrimmedMeanFilterTest {

	@Test
	public void filterDiscardsSamplesAtBothEnds() {
		assertFilterWorksAsExpected(TimeSyncUtils.createTolerableSortedSamples(0, 20));
	}

	@Test
	public void filterCanHandleUnsortedList() {
		assertFilterWorksAsExpected(TimeSyncUtils.createTolerableUnsortedSamples(0, 20));
	}

	private void assertFilterWorksAsExpected(final List<TimeSynchronizationSample> originalSamples) {
		// Arrange:
		final AlphaTrimmedMeanFilter filter = new AlphaTrimmedMeanFilter();

		// Act:
		final List<TimeSynchronizationSample> samples = filter.filter(originalSamples, null);

		// Assert:
		final int value = (int)(originalSamples.size() * FilterConstants.ALPHA / 2);
		Assert.assertThat(samples.size(), IsEqual.equalTo(originalSamples.size() - 2 * value));
		Assert.assertThat(samples, IsEqual.equalTo(TimeSyncUtils.createTolerableSortedSamples(value, originalSamples.size() - 2 * value)));
	}
}
