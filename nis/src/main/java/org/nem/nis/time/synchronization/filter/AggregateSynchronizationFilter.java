package org.nem.nis.time.synchronization.filter;

import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.time.synchronization.SynchronizationSample;

import java.util.*;
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
		// TODO 20140823 BR: this looks ugly. Any better way to do it?
		// TODO J-B: sometimes a regular for loop is more readable (i think this is one of those times :))
		final List<List<SynchronizationSample>> samplesList = Arrays.asList(samples);
		filters.stream().forEach(f -> samplesList.set(0, f.filter(samplesList.get(0), age)));

		return samplesList.get(0);
	}
}
