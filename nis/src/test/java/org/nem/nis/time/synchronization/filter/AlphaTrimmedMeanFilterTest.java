package org.nem.nis.time.synchronization.filter;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.nis.test.TimeSyncUtils;
import org.nem.nis.time.synchronization.SynchronizationSample;

import java.util.List;

public class AlphaTrimmedMeanFilterTest {

	@Test
	public void filterDiscardsSamplesAtBothEnds() {
		assertFilterWorksAsExpected(TimeSyncUtils.createTolerableSortedSamples(0, 20));
		// TODO-CR: J-B instead of using true/false for sorted/unsorted, consider separate functions like createTolerableUnsortedSamples
		// TODO     BR -> J done.
	}

	@Test
	public void filterCanHandleUnsortedList() {
		assertFilterWorksAsExpected(TimeSyncUtils.createTolerableUnsortedSamples(0, 20));
	}

	private void assertFilterWorksAsExpected(final List<SynchronizationSample> originalSamples) {
		// Arrange:
		final AlphaTrimmedMeanFilter filter = new AlphaTrimmedMeanFilter();

		// Act:
		final List<SynchronizationSample> samples = filter.filter(originalSamples, null);

		// Assert:
		final int value = (int)(originalSamples.size() * FilterConstants.ALPHA / 2);
		Assert.assertThat(samples.size(), IsEqual.equalTo(originalSamples.size() - 2 * value));
		Assert.assertThat(samples, IsEqual.equalTo(TimeSyncUtils.createTolerableSortedSamples(value, originalSamples.size() - 2 * value)));
	}
}
