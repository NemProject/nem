package org.nem.nis.time.synchronization.filter;

import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.time.synchronization.SynchronizationSample;

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
	public List<SynchronizationSample> filter(final List<SynchronizationSample> samples, final NodeAge age) {
		List<SynchronizationSample> filteredSamples = samples;
		for (SynchronizationFilter filter : this.filters) {
			filteredSamples = filter.filter(filteredSamples, age);
		}

		return filteredSamples;
	}
}
