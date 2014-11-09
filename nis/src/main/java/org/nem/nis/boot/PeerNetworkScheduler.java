package org.nem.nis.boot;

import org.nem.core.async.*;
import org.nem.core.model.Block;
import org.nem.core.node.NodeApiId;
import org.nem.core.time.TimeProvider;
import org.nem.nis.BlockChain;
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
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder()
				.setRecurringFutureSupplier(
						this.runnableToFutureSupplier(() -> {
							final Block block = harvester.harvestBlock();
							if (null == block || !blockChain.processBlock(block).isSuccess()) {
								return;
							}

							final SecureSerializableEntity<?> secureBlock = new SecureSerializableEntity<>(
									block,
									network.getLocalNode().getIdentity());
							network.broadcast(NodeApiId.REST_PUSH_BLOCK, secureBlock);
						}))
				.setInitialDelay(FORAGING_INITIAL_DELAY)
				.setDelayStrategy(new UniformDelayStrategy(FORAGING_INTERVAL))
				.setVisitor(timerVisitor)
				.create();
		this.timers.add(new AsyncTimer(options));
	}

	private void addNetworkTasks(final PeerNetwork network, final boolean useNetworkTime) {
		final NetworkTaskInitializer initializer = new NetworkTaskInitializer(this);
		initializer.initialize(network, useNetworkTime);
	}

	private static class NetworkTaskInitializer {
		private final PeerNetworkScheduler scheduler;
		private AsyncTimer refreshTimer;

		public NetworkTaskInitializer(final PeerNetworkScheduler scheduler) {
			this.scheduler = scheduler;
		}

		private void initialize(final PeerNetwork network, final boolean useNetworkTime) {
			this.refreshTimer = this.addRefreshTask(network);

			if (useNetworkTime) {
				this.addSimpleTask(
						() -> network.synchronizeTime(this.scheduler.timeProvider),
						getTimeSynchronizationDelayStrategy(),
						"TIME SYNCHRONIZATION");
			}

			this.addSimpleTask(
					() -> network.broadcast(NodeApiId.REST_NODE_PING, network.getLocalNodeAndExperiences()),
					BROADCAST_INTERVAL,
					"BROADCAST");
			this.addSimpleTask(
					this.scheduler.runnableToFutureSupplier(() -> network.synchronize()),
					SYNC_INTERVAL,
					"SYNC");
			this.addSimpleTask(
					this.scheduler.runnableToFutureSupplier(() -> network.pruneInactiveNodes()),
					PRUNE_INACTIVE_NODES_DELAY,
					"PRUNING INACTIVE NODES");
			this.addSimpleTask(
					this.scheduler.runnableToFutureSupplier(() -> network.updateLocalNodeEndpoint()),
					UPDATE_LOCAL_NODE_ENDPOINT_DELAY,
					"UPDATING LOCAL NODE ENDPOINT");
			this.addSimpleTask(
					() -> network.checkChainSynchronization(),
					CHECK_CHAIN_SYNC_INTERVAL,
					"CHECKING CHAIN SYNCHRONIZATION");
		}

		private AsyncTimer addRefreshTask(final PeerNetwork network) {
			final AsyncTimerOptionsBuilder builder = new AsyncTimerOptionsBuilder()
					.setRecurringFutureSupplier(() -> network.refresh())
					.setInitialDelay(REFRESH_INITIAL_DELAY)
					.setDelayStrategy(getRefreshDelayStrategy());
			return this.addTask("REFRESH", builder);
		}

		private void addSimpleTask(
				final Supplier<CompletableFuture<?>> recurringFutureSupplier,
				final int delay,
				final String name) {
			this.addSimpleTask(recurringFutureSupplier, new UniformDelayStrategy(delay), name);
		}

		private void addSimpleTask(
				final Supplier<CompletableFuture<?>> recurringFutureSupplier,
				final AbstractDelayStrategy delay,
				final String name) {
			final AsyncTimerOptionsBuilder builder = new AsyncTimerOptionsBuilder()
					.setRecurringFutureSupplier(recurringFutureSupplier)
					.setTrigger(this.refreshTimer.getFirstFireFuture())
					.setInitialDelay(REFRESH_INITIAL_DELAY * this.scheduler.timerVisitors.size()) // stagger the timer start times
					.setDelayStrategy(delay);
			this.addTask(name, builder);
		}

		private AsyncTimer addTask(final String name, final AsyncTimerOptionsBuilder builder) {
			final AsyncTimerVisitor timerVisitor = this.scheduler.createNamedVisitor(name);
			builder.setVisitor(timerVisitor);

			final AsyncTimer timer = new AsyncTimer(builder.create());
			this.scheduler.timers.add(timer);
			return timer;
		}
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
