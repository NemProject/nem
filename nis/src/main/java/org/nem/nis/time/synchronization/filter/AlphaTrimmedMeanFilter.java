package org.nem.nis.time.synchronization.filter;

import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.time.synchronization.SynchronizationSample;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filters out a given percentage of samples at the upper and lower bound of time offset.
 */
public class AlphaTrimmedMeanFilter implements SynchronizationFilter {

	// TODO J-B: question - so in this filter, you are dropping the low and high outliers?
	// TODO BR -> J yes, an attacker will place his samples at either end of the sorted list,
	// TODO       filtering will help to fight back the attack.

	@Override
	public List<SynchronizationSample> filter(final List<SynchronizationSample> samples, final NodeAge age) {
		final int samplesToDiscardAtBothEnds = (int)(samples.size() * FilterConstants.ALPHA / 2);
		// TODO: 20140823 BR: is there a way to do this with samples.stream()?
		// TODO J-B: i think you can use sorted, skip, and limit
		// TODO BR -> J thx.
		return samples.stream()
				.sorted()
				.skip(samplesToDiscardAtBothEnds)
				.limit(samples.size() - 2 * samplesToDiscardAtBothEnds)
				.collect(Collectors.toList());
	}
}
