package org.nem.nis.time.synchronization.filter;

import org.nem.core.model.primitive.NodeAge;
import org.nem.core.time.synchronization.TimeSynchronizationSample;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filters out a given percentage of samples at the upper and lower bound of time offset.
 */
public class AlphaTrimmedMeanFilter implements SynchronizationFilter {

	@Override
	public List<TimeSynchronizationSample> filter(final List<TimeSynchronizationSample> samples, final NodeAge age) {
		final int samplesToDiscardAtBothEnds = (int) (samples.size() * FilterConstants.ALPHA / 2);
		return samples.stream().sorted().skip(samplesToDiscardAtBothEnds).limit(samples.size() - 2 * samplesToDiscardAtBothEnds)
				.collect(Collectors.toList());
	}
}
