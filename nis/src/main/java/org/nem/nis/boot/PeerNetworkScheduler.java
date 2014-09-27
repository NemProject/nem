package org.nem.nis.boot;

import org.nem.core.async.*;
import org.nem.core.model.*;
import org.nem.core.node.NodeApiId;
import org.nem.core.time.TimeProvider;
import org.nem.nis.*;
import org.nem.nis.harvesting.Harvester;
import org.nem.peer.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Scheduler that keeps track of all scheduled, recurring timers.
 */
public class PeerNetworkScheduler implements AutoCloseable {
	private static final int ONE_SECOND = 1000;
	private static final int ONE_MINUTE = 60 * ONE_SECOND;
	private static final int ONE_HOUR = 60 * ONE_MINUTE;

	private static final int REFRESH_INITIAL_DELAY = 200;
	private static final int REFRESH_INITIAL_INTERVAL = ONE_SECOND;
	private static final int REFRESH_PLATEAU_INTERVAL = 5 * ONE_MINUTE;
	private static final int REFRESH_BACK_OFF_TIME = 12 * ONE_HOUR;

	private static final int SYNC_INTERVAL = 5 * ONE_SECOND;

	private static final int BROADCAST_INTERVAL = 5 * ONE_MINUTE;

	private static final int FORAGING_INITIAL_DELAY = 5 * ONE_SECOND;
	private static final int FORAGING_INTERVAL = 3 * ONE_SECOND;

	private static final int PRUNE_INACTIVE_NODES_DELAY = ONE_HOUR;

	private static final int UPDATE_LOCAL_NODE_ENDPOINT_DELAY = 5 * ONE_MINUTE;

	private static final int TIME_SYNC_INITIAL_INTERVAL = ONE_MINUTE;
	private static final int TIME_SYNC_INITIAL_INTERVAL_ROUNDS = 15;
	private static final int TIME_SYNC_PLATEAU_INTERVAL = ONE_HOUR;
	private static final int TIME_SYNC_BACK_OFF_TIME = 6 * ONE_HOUR;

	private static final int CHECK_CHAIN_SYNC_INTERVAL = 30 * ONE_SECOND;

	private final TimeProvider timeProvider;
	private final List<NemAsyncTimerVisitor> timerVisitors = new ArrayList<>();
	private final List<AsyncTimer> timers = new ArrayList<>();
	private final Executor executor = Executors.newCachedThreadPool();

	/**
	 * Creates a new scheduler.
	 *
	 * @param timeProvider The time provider.
	 */
	public PeerNetworkScheduler(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	/**
	 * Gets all timer visitors.
	 *
	 * @return All timer visitors.
	 */
	public List<NemAsyncTimerVisitor> getVisitors() {
		return this.timerVisitors;
	}

	/**
	 * Adds all NIS tasks.
	 *
	 * @param network The network.
	 * @param blockChain The block chain.
	 * @param harvester The harvester.
	 * @param useNetworkTime true if network time should be used.
	 */
	public void addTasks(
			final PeerNetwork network,
			final BlockChain blockChain,
			final Harvester harvester,
			final boolean useNetworkTime) {
		this.addForagingTask(network, blockChain, harvester);
		this.addNetworkTasks(network, useNetworkTime);
	}

	private void addForagingTask(final PeerNetwork network, final BlockChain blockChain, final Harvester harvester) {
		final AsyncTimerVisitor timerVisitor = this.createNamedVisitor("FORAGING");
		this.timers.add(new AsyncTimer(
				this.runnableToFutureSupplier(() -> {
					final Block block = harvester.harvestBlock();
					if (null == block || !blockChain.processBlock(block).isSuccess()) {
						return;
					}

					final SecureSerializableEntity<?> secureBlock = new SecureSerializableEntity<>(
							block,
							network.getLocalNode().getIdentity());
					network.broadcast(NodeApiId.REST_PUSH_BLOCK, secureBlock);
				}),
				FORAGING_INITIAL_DELAY,
				new UniformDelayStrategy(FORAGING_INTERVAL),
				timerVisitor));
	}

	private void addNetworkTasks(final PeerNetwork network, final boolean useNetworkTime) {
		this.addRefreshTask(network);
		if (useNetworkTime) {
			this.addTimeSynchronizationTask(network);
		}

		this.addSimpleTask(
				() -> network.broadcast(NodeApiId.REST_NODE_PING, network.getLocalNodeAndExperiences()),
				BROADCAST_INTERVAL,
				"BROADCAST");
		this.addSimpleTask(
				this.runnableToFutureSupplier(() -> network.synchronize()),
				SYNC_INTERVAL,
				"SYNC");
		this.addSimpleTask(
				this.runnableToFutureSupplier(() -> network.pruneInactiveNodes()),
				PRUNE_INACTIVE_NODES_DELAY,
				"PRUNING INACTIVE NODES");
		this.addSimpleTask(
				this.runnableToFutureSupplier(() -> network.updateLocalNodeEndpoint()),
				UPDATE_LOCAL_NODE_ENDPOINT_DELAY,
				"UPDATING LOCAL NODE ENDPOINT");
		this.addSimpleTask(
				this.runnableToFutureSupplier(() -> network.checkChainSynchronization()),
				CHECK_CHAIN_SYNC_INTERVAL,
				"CHECKING CHAIN SYNCHRONIZATION");
	}

	private void addRefreshTask(final PeerNetwork network) {
		final AsyncTimerVisitor timerVisitor = this.createNamedVisitor("REFRESH");
		this.timers.add(new AsyncTimer(
				() -> network.refresh(),
				REFRESH_INITIAL_DELAY * this.timerVisitors.size(), // stagger the timer start times
				getRefreshDelayStrategy(),
				timerVisitor));
	}

	private void addTimeSynchronizationTask(final PeerNetwork network) {
		final AsyncTimerVisitor timerVisitor = this.createNamedVisitor("TIME SYNCHRONIZATION");
		this.timers.add(new AsyncTimer(
				() -> network.synchronizeTime(this.timeProvider),
				REFRESH_INITIAL_DELAY * this.timerVisitors.size(), // stagger the timer start times
				getTimeSynchronizationDelayStrategy(),
				timerVisitor));
	}

	private void addSimpleTask(
			final Supplier<CompletableFuture<?>> recurringFutureSupplier,
			final int delay,
			final String name) {
		final AsyncTimerVisitor timerVisitor = this.createNamedVisitor(name);

		this.timers.add(new AsyncTimer(
				recurringFutureSupplier,
				REFRESH_INITIAL_DELAY,
				new UniformDelayStrategy(delay),
				timerVisitor));
	}

	private static AbstractDelayStrategy getRefreshDelayStrategy() {
		// initially refresh at REFRESH_INITIAL_INTERVAL (1s), gradually increasing to
		// REFRESH_PLATEAU_INTERVAL (5m) over REFRESH_BACK_OFF_TIME (12 hours),
		// and then plateau at that rate forever
		final List<AbstractDelayStrategy> subStrategies = Arrays.asList(
				LinearDelayStrategy.withDuration(
						REFRESH_INITIAL_INTERVAL,
						REFRESH_PLATEAU_INTERVAL,
						REFRESH_BACK_OFF_TIME),
				new UniformDelayStrategy(REFRESH_PLATEAU_INTERVAL));
		return new AggregateDelayStrategy(subStrategies);
	}

	private static AbstractDelayStrategy getTimeSynchronizationDelayStrategy() {
		// initially refresh at TIME_SYNC_INITIAL_INTERVAL (1min), keeping it for TIME_SYNC_INITIAL_INTERVAL_ROUNDS rounds,
		// then gradually increasing to TIME_SYNC_PLATEAU_INTERVAL (1h) over TIME_SYNC_BACK_OFF_TIME (6 hours),
		// and then plateau at that rate forever
		final List<AbstractDelayStrategy> subStrategies = Arrays.asList(
				new UniformDelayStrategy(TIME_SYNC_INITIAL_INTERVAL, TIME_SYNC_INITIAL_INTERVAL_ROUNDS),
				LinearDelayStrategy.withDuration(
						TIME_SYNC_INITIAL_INTERVAL,
						TIME_SYNC_PLATEAU_INTERVAL,
						TIME_SYNC_BACK_OFF_TIME),
				new UniformDelayStrategy(TIME_SYNC_PLATEAU_INTERVAL));
		return new AggregateDelayStrategy(subStrategies);
	}

	@Override
	public void close() {
		this.timers.forEach(AsyncTimer::close);
	}

	private Supplier<CompletableFuture<?>> runnableToFutureSupplier(final Runnable runnable) {
		return () -> CompletableFuture.runAsync(runnable, this.executor);
	}

	private NemAsyncTimerVisitor createNamedVisitor(final String name) {
		final NemAsyncTimerVisitor timerVisitor = new NemAsyncTimerVisitor(name, this.timeProvider);
		this.timerVisitors.add(timerVisitor);
		return timerVisitor;
	}
}
