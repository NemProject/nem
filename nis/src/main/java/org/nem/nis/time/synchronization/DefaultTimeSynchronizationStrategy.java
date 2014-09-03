package org.nem.nis.time.synchronization;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.time.synchronization.TimeSynchronizationSample;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.secret.AccountImportance;
import org.nem.nis.time.synchronization.filter.SynchronizationFilter;

import java.util.List;

/**
 * The default implementation for the synchronization strategy based on the thesis
 * Algorithms and Services for Peer-to-Peer Internal Clock Synchronization:
 * http://www.dis.uniroma1.it/~dottoratoii/media/students/documents/thesis_scipioni.pdf
 */
public class DefaultTimeSynchronizationStrategy implements TimeSynchronizationStrategy {

	private final SynchronizationFilter filter;
	private final PoiFacade poiFacade;

	/**
	 * Creates the default synchronization strategy.
	 *
	 * @param filter The aggregate filter to use.
	 * @param poiFacade The poi facade to query account importances.
	 */
	public DefaultTimeSynchronizationStrategy(final SynchronizationFilter filter, final PoiFacade poiFacade) {
		if (null == filter) {
			throw new TimeSynchronizationException("synchronization filter cannot be null.");
		}
		if (null == poiFacade) {
			throw new TimeSynchronizationException("poiFacade cannot be null.");
		}
		this.filter = filter;
		this.poiFacade = poiFacade;
	}

	/**
	 * Gets a value indicating how strong the coupling should be.
	 * Starting value should be chosen such that coupling is strong to achieve a fast convergence in the beginning.
	 * Minimum value should be chosen such that the network time shows some inertia.
	 * TODO 20140825 BR: Should the coupling be dependent on the trust in a node?
	 * TODO J-B i think a node's trust similarity should influence coupling; i was also wondering if we could piggyback on the nodes returned by the NodeSelector
	 * TODO BR -> J I think the only way to withstand a sybil attack is to additionally tie the coupling to the remote nodes importance.
	 * TODO         Attacking nodes will have a low importance and thus a low coupling.
	 * TODO J-B question - is this function public just for the testing?
	 * TODO BR -> J yeah, didn't see another way to do testing.
	 *
	 * @param age The node's age.
	 * @return The coupling.
	 */
	public double getCoupling(final NodeAge age) {
		final long ageToUse = Math.max(age.getRaw() - TimeSynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND, 0);
		return Math.max(Math.exp(-TimeSynchronizationConstants.COUPLING_DECAY_STRENGTH * ageToUse) * TimeSynchronizationConstants.COUPLING_START,
				TimeSynchronizationConstants.COUPLING_MINIMUM);
	}

	private double getAccountImportance(final Address address) {
		final AccountImportance importanceInfo = this.poiFacade.findStateByAddress(address).getImportanceInfo();
		return importanceInfo.getImportance(importanceInfo.getHeight());
	}

	@Override
	public TimeOffset calculateTimeOffset(final List<TimeSynchronizationSample> samples, final NodeAge age) {
		final List<TimeSynchronizationSample> filteredSamples = this.filter.filter(samples, age);
		if (filteredSamples.isEmpty()) {
			throw new TimeSynchronizationException("No synchronization samples available to calculate network time.");
		}
		// TODO BR: not sure about this approach but it seems quite reasonable.
		final double cumulativeImportance = filteredSamples.stream().mapToDouble(s -> getAccountImportance(s.getNode().getIdentity().getAddress())).sum();
		final double viewSizePercentage = (double)filteredSamples.size() / (double)this.poiFacade.getLastPoiVectorSize();
		final double scaling = cumulativeImportance > viewSizePercentage? 1 / cumulativeImportance : 1 / viewSizePercentage;
		final double sum = filteredSamples.stream()
				.mapToDouble(s -> {
					final double importance = getAccountImportance(s.getNode().getIdentity().getAddress());
					return s.getTimeOffsetToRemote() * importance * scaling; })
				.sum();

		return new TimeOffset((long)(sum * this.getCoupling(age)));
	}
}
