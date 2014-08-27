package org.nem.nis.time.synchronization;

import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.time.synchronization.filter.SynchronizationFilter;

import java.util.List;

/**
 * The default implementation for the synchronization strategy based on the thesis
 * Algorithms and Services for Peer-to-Peer Internal Clock Synchronization:
 * http://www.dis.uniroma1.it/~dottoratoii/media/students/documents/thesis_scipioni.pdf
 */
public class DefaultSynchronizationStrategy implements SynchronizationStrategy {

	private final SynchronizationFilter filter;

	public DefaultSynchronizationStrategy(final SynchronizationFilter filter) {
		if (null == filter) {
			throw new SynchronizationException("synchronization filter cannot be null.");
		}
		this.filter = filter;
	}

	/**
	 * Gets a value indicating maximum deviation before clamping occurs.
	 * TODO 20140825 BR: Should the coupling be dependent on the trust in a node?
	 *
	 * @param age The node's age.
	 * @return The coupling.
	 */
	public double getCoupling(final NodeAge age) {
		final long ageToUse = Math.max(age.getRaw() - SynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND, 0);
		return Math.max(Math.exp(-SynchronizationConstants.COUPLING_DECAY_STRENGTH * ageToUse) * SynchronizationConstants.COUPLING_START,
				SynchronizationConstants.COUPLING_MINIMUM);
	}

	@Override
	public long calculateTimeOffset(final List<SynchronizationSample> samples, final NodeAge age) {
		final List<SynchronizationSample> filteredSamples = this.filter.filter(samples, age);
		if (filteredSamples.isEmpty()) {
			throw new SynchronizationException("No synchronization samples available to calculate network time.");
		}
		final long sum = filteredSamples.stream().mapToLong(SynchronizationSample::getTimeOffsetToRemote).sum();

		return (long) ((sum * this.getCoupling(age)) / filteredSamples.size());
	}
}
