package org.nem.nis.time.synchronization.filter;

import org.nem.core.model.primitive.NodeAge;
import org.nem.core.time.synchronization.TimeSynchronizationSample;

import java.util.List;

/**
 * Aggregate synchronization filter.
 */
public class AggregateSynchronizationFilter implements SynchronizationFilter {
	private final List<SynchronizationFilter> filters;

	public AggregateSynchronizationFilter(List<SynchronizationFilter> filters) {
		this.filters = filters;
	}

	@Override
	public List<TimeSynchronizationSample> filter(final List<TimeSynchronizationSample> samples, final NodeAge age) {
		List<TimeSynchronizationSample> filteredSamples = samples;
		for (SynchronizationFilter filter : this.filters) {
			filteredSamples = filter.filter(filteredSamples, age);
		}

		return filteredSamples;
	}
}
