package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.core.time.*;

import java.util.logging.Logger;

/**
 * TrustProvider decorator that caches trust calculation results for a specified period of time.
 */
public class CachedTrustProvider implements TrustProvider {
	private static final Logger LOGGER = Logger.getLogger(CachedTrustProvider.class.getName());

	private final TrustProvider trustProvider;
	private final int cacheTime;
	private final TimeProvider timeProvider;
	private final Object lock = new Object();
	private TimeInstant lastCacheTime;
	private TrustResult lastTrustResult;

	/**
	 * Creates a new trust provider mask decorator.
	 *
	 * @param trustProvider The trust provider.
	 * @param cacheTime The amount of time to cache trust values
	 * @param timeProvider The time provider.
	 */
	public CachedTrustProvider(
			final TrustProvider trustProvider,
			final int cacheTime,
			final TimeProvider timeProvider) {
		this.trustProvider = trustProvider;
		this.cacheTime = cacheTime;
		this.timeProvider = timeProvider;
	}

	@Override
	public TrustResult computeTrust(final TrustContext context) {
		// there shouldn't be much contention on this lock and it's better to prevent
		// multiple simultaneous trust calculations
		synchronized (this.lock) {
			final TimeInstant currentTime = this.timeProvider.getCurrentTime();
			if (null == this.lastTrustResult || currentTime.subtract(this.lastCacheTime) > this.cacheTime) {
				LOGGER.info("calculating trust values");
				this.lastCacheTime = currentTime;
				this.lastTrustResult = this.trustProvider.computeTrust(context);
				this.lastTrustResult.getTrustValues().normalize();
				LOGGER.info(String.format("trust calculation finished (%d values)", this.lastTrustResult.getTrustValues().size()));
			}

			// return a copy of the trust values
			return new TrustResult(
					this.lastTrustResult.getTrustContext(),
					this.lastTrustResult.getTrustValues().add(0));
		}
	}
}
