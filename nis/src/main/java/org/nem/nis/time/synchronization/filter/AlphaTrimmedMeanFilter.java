package org.nem.nis.time.synchronization.filter;

import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.time.synchronization.SynchronizationSample;

import java.util.*;

/**
 * Filters out a given percentage of samples at the upper and lower bound of time offset.
 */
public class AlphaTrimmedMeanFilter implements SynchronizationFilter {

	@Override
	public List<SynchronizationSample> filter(final List<SynchronizationSample> samples, final NodeAge age) {
		final int samplesToDiscardAtBothEnds = (int)(samples.size() * FilterConstants.ALPHA / 2);
		// TODO: 20140823 BR: is there a way to do this with samples.stream()?
		Collections.sort(samples);
		List<SynchronizationSample> trimmedSamples = new ArrayList<>();
		for (int i=samplesToDiscardAtBothEnds; i<samples.size() - samplesToDiscardAtBothEnds; i++) {
			trimmedSamples.add(samples.get(i));
		}

		return trimmedSamples;
	}
}
