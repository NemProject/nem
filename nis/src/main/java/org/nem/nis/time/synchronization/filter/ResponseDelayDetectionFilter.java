package org.nem.nis.time.synchronization.filter;

import org.nem.core.model.primitive.NodeAge;
import org.nem.core.time.synchronization.TimeSynchronizationSample;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A filter that filters samples that indicate an unexpected delay in the response (e.g. due to garbage collection).
 */
public class ResponseDelayDetectionFilter implements SynchronizationFilter {

	@Override
	public List<TimeSynchronizationSample> filter(final List<TimeSynchronizationSample> samples, final NodeAge age) {
		return samples.stream().filter(s -> FilterConstants.TOLERATED_DURATION_MAXIMUM >= s.getDuration()).collect(Collectors.toList());
	}
}
