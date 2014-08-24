package org.nem.nis.time.synchronization.filter;

import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.time.synchronization.SynchronizationSample;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filters out samples that have a non tolerable high time offset.
 */
public class ClampingFilter implements SynchronizationFilter {

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
	public List<SynchronizationSample> filter(final List<SynchronizationSample> samples, final NodeAge age) {
		final long toleratedDeviation = getMaximumToleratedDeviation(age);

		return samples.stream()
				.filter(s -> (s.getTimeOffsetToRemote() <= toleratedDeviation && -toleratedDeviation <= s.getTimeOffsetToRemote()))
				.collect(Collectors.toList());
	}
}
