package org.nem.peer.trust;

import org.nem.core.node.Node;
import org.nem.core.time.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * TrustProvider decorator that caches trust calculation results for a specified period of time.
 */
public class CachedTrustProvider implements TrustProvider {
	private static final Logger LOGGER = Logger.getLogger(CachedTrustProvider.class.getName());
	private static final int MAX_MATRIX_SIZE = 101;
	private static final int TOP_TRUSTED_NODES_TO_KEEP = 10;

	private final TrustProvider trustProvider;
	private final int cacheTime;
	private final TimeProvider timeProvider;
	private final Object lock = new Object();
	private final SecureRandom random = new SecureRandom();

	private TimeInstant lastCacheTime;
	private TrustResult lastTrustResult;

	/**
	 * Creates a new trust provider mask decorator.
	 *
	 * @param trustProvider The trust provider.
	 * @param cacheTime The amount of time to cache trust values
	 * @param timeProvider The time provider.
	 */
	public CachedTrustProvider(final TrustProvider trustProvider, final int cacheTime, final TimeProvider timeProvider) {
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
				this.lastTrustResult = this.trustProvider.computeTrust(this.truncateContext(context));
				this.lastTrustResult.getTrustValues().normalize();
				LOGGER.info(String.format("trust calculation finished (%d values)", this.lastTrustResult.getTrustValues().size()));
			}

			// return a copy of the trust values
			return new TrustResult(this.lastTrustResult.getTrustContext(), this.lastTrustResult.getTrustValues().add(0));
		}
	}

	private TrustContext truncateContext(final TrustContext context) {
		final Node[] contextNodes = context.getNodes();
		if (contextNodes.length <= MAX_MATRIX_SIZE) {
			return context;
		}

		LOGGER.info(String.format("truncating trust calculation from %d nodes", contextNodes.length));
		final List<Node> truncatedNodes = new ArrayList<>();
		final Set<Node> currentNodes = new HashSet<>(Arrays.asList(contextNodes));
		List<Tuple> tuples = this.mapNodesToTuples(currentNodes);

		// select the top TOP_TRUSTED_NODES_TO_KEEP nodes with the highest trust
		if (null != this.lastTrustResult) {
			final Node[] nodesFromLastResult = this.lastTrustResult.getTrustContext().getNodes();
			final List<Tuple> lastResultTuples = this.mapNodesToTuples(Arrays.asList(nodesFromLastResult));
			final List<Node> topTrustedNodes = lastResultTuples.stream().sorted((l, r) -> -1 * Double.compare(l.trust, r.trust))
					.map(t -> t.node).limit(TOP_TRUSTED_NODES_TO_KEEP).filter(currentNodes::contains).collect(Collectors.toList());

			truncatedNodes.addAll(topTrustedNodes);
			currentNodes.removeAll(truncatedNodes);
			tuples = this.mapNodesToTuples(currentNodes);
		}

		// select random nodes from the full population
		final int numRemainingNodes = MAX_MATRIX_SIZE - truncatedNodes.size() - 1; // subtract 1 for local node
		final List<Node> randomNodes = tuples.stream().sorted((l, r) -> Double.compare(l.random, r.random)).map(t -> t.node)
				.limit(numRemainingNodes).collect(Collectors.toList());
		truncatedNodes.addAll(randomNodes);

		// always add the local node
		truncatedNodes.add(context.getLocalNode());

		return new TrustContext(truncatedNodes.toArray(new Node[truncatedNodes.size()]), context.getLocalNode(),
				context.getNodeExperiences(), context.getPreTrustedNodes(), context.getParams());
	}

	private List<Tuple> mapNodesToTuples(final Collection<Node> nodes) {
		final List<Tuple> tuples = new ArrayList<>();
		for (final Node node : nodes) {
			final Tuple tuple = new Tuple();
			tuple.node = node;
			tuple.trust = this.findCachedTrust(tuple.node);
			tuple.random = this.random.nextDouble();
			tuples.add(tuple);
		}

		return tuples;
	}

	private double findCachedTrust(final Node node) {
		final Node[] originalNodes = null == this.lastTrustResult ? new Node[]{} : this.lastTrustResult.getTrustContext().getNodes();
		for (int i = 0; i < originalNodes.length - 1; ++i) {
			if (originalNodes[i].equals(node)) {
				return this.lastTrustResult.getTrustValues().getAt(i);
			}
		}

		return 0;
	}

	private static class Tuple {
		public Node node;
		public double trust;
		public double random;
	}
}
