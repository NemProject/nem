package org.nem.nis.time.synchronization.filter;

import java.util.List;
import org.nem.core.model.primitive.NodeAge;
import org.nem.core.time.synchronization.TimeSynchronizationSample;

/**
 * Aggregate synchronization filter.
 */
public class AggregateSynchronizationFilter implements SynchronizationFilter {
	private final List<SynchronizationFilter> filters;

	public AggregateSynchronizationFilter(final List<SynchronizationFilter> filters) {
		this.filters = filters;
	}

	@Override
	public List<TimeSynchronizationSample> filter(final List<TimeSynchronizationSample> samples, final NodeAge age) {
		List<TimeSynchronizationSample> filteredSamples = samples;
		for (final SynchronizationFilter filter : this.filters) {
			filteredSamples = filter.filter(filteredSamples, age);
		}

		return filteredSamples;
	}
}
