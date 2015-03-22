package org.nem.nis.time.synchronization;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.time.synchronization.TimeSynchronizationSample;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyAccountImportance;
import org.nem.nis.time.synchronization.filter.SynchronizationFilter;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.*;

/**
 * The default implementation for the synchronization strategy based on the thesis
 * Algorithms and Services for Peer-to-Peer Internal Clock Synchronization:
 * http://www.dis.uniroma1.it/~dottoratoii/media/students/documents/thesis_scipioni.pdf
 */
public class DefaultTimeSynchronizationStrategy implements TimeSynchronizationStrategy {
	private static final Logger LOGGER = Logger.getLogger(DefaultTimeSynchronizationStrategy.class.getName());
	private static final Integer WARNING_THRESHOLD_MILLIS = 100;

	private final SynchronizationFilter filter;
	private final ReadOnlyPoiFacade poiFacade;
	private final ReadOnlyAccountStateCache accountStateCache;
	private final BiConsumer<Integer, String> logger;

	/**
	 * Creates the default synchronization strategy.
	 *
	 * @param filter The aggregate filter to use.
	 * @param poiFacade The poi facade.
	 * @param accountStateCache The account state cache.
	 */
	public DefaultTimeSynchronizationStrategy(
			final SynchronizationFilter filter,
			final ReadOnlyPoiFacade poiFacade,
			final ReadOnlyAccountStateCache accountStateCache) {
		this(filter, poiFacade, accountStateCache, (o, s) -> {
			if ((WARNING_THRESHOLD_MILLIS.compareTo(Math.abs(o))) > 0) {
				LOGGER.log(Level.INFO, s);
			} else {
				LOGGER.log(Level.WARNING, s);
			}
		});
	}

	/**
	 * Creates the default synchronization strategy.
	 *
	 * @param filter The aggregate filter to use.
	 * @param poiFacade The poi facade.
	 * @param accountStateCache The account state cache.
	 * @param logger The consumer which optionally logs calculated time offsets.
	 */
	public DefaultTimeSynchronizationStrategy(
			final SynchronizationFilter filter,
			final ReadOnlyPoiFacade poiFacade,
			final ReadOnlyAccountStateCache accountStateCache,
			final BiConsumer<Integer, String> logger) {
		if (null == filter) {
			throw new TimeSynchronizationException("synchronization filter cannot be null.");
		}

		if (null == poiFacade) {
			throw new TimeSynchronizationException("accountStateCache cannot be null.");
		}

		this.filter = filter;
		this.poiFacade = poiFacade;
		this.accountStateCache = accountStateCache;
		this.logger = logger;
	}

	/**
	 * Gets a value indicating how strong the coupling should be.
	 * Starting value should be chosen such that coupling is strong to achieve a fast convergence in the beginning.
	 * Minimum value should be chosen such that the network time shows some inertia.
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
		final ReadOnlyAccountImportance importanceInfo = this.accountStateCache.findLatestForwardedStateByAddress(address).getImportanceInfo();
		return importanceInfo.getImportance(importanceInfo.getHeight());
	}

	@Override
	public TimeOffset calculateTimeOffset(final List<TimeSynchronizationSample> samples, final NodeAge age) {
		final List<TimeSynchronizationSample> filteredSamples = this.filter.filter(samples, age);
		if (filteredSamples.isEmpty()) {
			throw new TimeSynchronizationException("No synchronization samples available to calculate network time.");
		}

		final double cumulativeImportance = filteredSamples.stream().mapToDouble(s -> this.getAccountImportance(s.getNode().getIdentity().getAddress())).sum();
		final double viewSizePercentage = (double)filteredSamples.size() / (double)this.poiFacade.getLastPoiVectorSize();
		final double scaling = cumulativeImportance > viewSizePercentage ? 1 / cumulativeImportance : 1 / viewSizePercentage;
		final double sum = filteredSamples.stream()
				.mapToDouble(s -> {
					final long offset = s.getTimeOffsetToRemote();
					final String entry = String.format(
							"%s: network time offset to local node is %dms",
							s.getNode().getIdentity().getAddress().getEncoded(),
							offset);

					this.logger.accept((int)offset, entry);


					final double importance = this.getAccountImportance(s.getNode().getIdentity().getAddress());
					return offset * importance * scaling;
				})
				.sum();

		return new TimeOffset((long)(sum * this.getCoupling(age)));
	}
}
