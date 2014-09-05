package org.nem.nis.boot;

import org.nem.core.async.*;
import org.nem.core.model.Block;
import org.nem.core.node.NodeApiId;
import org.nem.core.time.TimeProvider;
import org.nem.nis.*;
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

	private static final int CHECK_CHAIN_SYNC_INTERVAL = 30 * ONE_SECOND;

	private final TimeProvider timeProvider;
	private final List<NisAsyncTimerVisitor> timerVisitors = new ArrayList<>();
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
	public List<NisAsyncTimerVisitor> getVisitors() {
		return this.timerVisitors;
	}

	/**
	 * Adds all NIS tasks.
	 *
	 * @param network The network.
	 * @param blockChain The block chain.
	 */
	public void addTasks(final PeerNetwork network, final BlockChain blockChain) {
		this.addForagingTask(network, blockChain);
		this.addNetworkTasks(network);
	}

	private void addForagingTask(final PeerNetwork network, final BlockChain blockChain) {
		final AsyncTimerVisitor timerVisitor = this.createNamedVisitor("FORAGING");
		this.timers.add(new AsyncTimer(
				this.runnableToFutureSupplier(() -> {
					final Block block = blockChain.forageBlock();
					if (null == block) {
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

	private void addNetworkTasks(final PeerNetwork network) {
		this.addRefreshTask(network);

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

	@Override
	public void close() {
		this.timers.forEach(AsyncTimer::close);
	}

	private Supplier<CompletableFuture<?>> runnableToFutureSupplier(final Runnable runnable) {
		return () -> CompletableFuture.runAsync(runnable, this.executor);
	}

	private NisAsyncTimerVisitor createNamedVisitor(final String name) {
		final NisAsyncTimerVisitor timerVisitor = new NisAsyncTimerVisitor(name, this.timeProvider);
		this.timerVisitors.add(timerVisitor);
		return timerVisitor;
	}
}
