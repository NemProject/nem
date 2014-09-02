package org.nem.nis.time.synchronization.filter;

import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.time.synchronization.TimeSynchronizationSample;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filters out samples that have a non tolerable high time offset.
 */
public class ClampingFilter implements SynchronizationFilter {

	// TODO J-B: question - so in this filter, you are reducing the tolerated deviation over time?
	// TODO J-B: question - i might have missed this, but how is age calculated?
	// TODO BR -> J the node age is incremented by 1 each time the node synchronizes. Nodes that recently
	// TODO         joined the network might be far off the already established network time. They need to
	// TODO         accept samples that are far off its own time in order to adapt to the network's logical clock.
	// TODO         On the other hand, matured nodes (high node age) should not be influenced by joining nodes
	// TODO         or attackers that provide samples which are far off the network's time.

	/**
	 * Gets a value indicating maximum deviation before clamping occurs.
	 *
	 * @param age The node's age.
	 * @return The maximum deviation.
	 */
	public long getMaximumToleratedDeviation(NodeAge age) {
		final long ageToUse = Math.max(age.getRaw() - FilterConstants.START_DECAY_AFTER_ROUND, 0);
		return (long)(Math.max(Math.exp(-FilterConstants.DECAY_STRENGTH * ageToUse) * FilterConstants.TOLERATED_DEVIATION_START, FilterConstants.TOLERATED_DEVIATION_MINIMUM));
	}

	@Override
	public List<TimeSynchronizationSample> filter(final List<TimeSynchronizationSample> samples, final NodeAge age) {
		final long toleratedDeviation = getMaximumToleratedDeviation(age);

		return samples.stream()
				.filter(s -> (s.getTimeOffsetToRemote() <= toleratedDeviation && -toleratedDeviation <= s.getTimeOffsetToRemote()))
				.collect(Collectors.toList());
	}
}
