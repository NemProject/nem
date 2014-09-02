package org.nem.nis.time.synchronization.filter;

import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.time.synchronization.TimeSynchronizationSample;

import java.util.List;

/**
 * Filters out synchronization samples that do not fulfill a predefined requirement.
 */
public interface SynchronizationFilter {

	/**
	 * Filters a list of synchronization samples.
	 *
	 * @param samples The list of samples.
	 * @return The filtered list of samples.
	 */
	public List<TimeSynchronizationSample> filter(final List<TimeSynchronizationSample> samples, final NodeAge age);
}
