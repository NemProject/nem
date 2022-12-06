package org.nem.nis.boot;

import org.nem.core.async.*;
import org.nem.core.node.NisPeerId;
import org.nem.core.time.TimeProvider;
import org.nem.nis.harvesting.HarvestingTask;
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
	private static final int REFRESH_INITIAL_INTERVAL = ONE_MINUTE;
	private static final int REFRESH_PLATEAU_INTERVAL = 5 * ONE_MINUTE;
	private static final int REFRESH_BACK_OFF_TIME = 12 * ONE_HOUR;

	private static final int SYNC_INTERVAL = 3 * ONE_SECOND;

	private static final int BROADCAST_INTERVAL = 5 * ONE_MINUTE;

	private static final int BROADCAST_BUFFERED_ENTITIES_INTERVAL = ONE_SECOND;

	private static final int FORAGING_INITIAL_DELAY = 5 * ONE_SECOND;
	private static final int FORAGING_INTERVAL = ONE_SECOND;

	private static final int PRUNE_INACTIVE_NODES_DELAY = ONE_HOUR;

	private static final int AUTO_IP_DETECTION_DELAY = 5 * ONE_MINUTE;

	private static final int TIME_SYNC_INITIAL_INTERVAL = ONE_MINUTE;
	private static final int TIME_SYNC_INITIAL_INTERVAL_ROUNDS = 15;
	private static final int TIME_SYNC_PLATEAU_INTERVAL = 3 * ONE_HOUR;
	private static final int TIME_SYNC_BACK_OFF_TIME = 9 * ONE_HOUR;

	private static final int NODE_EXPERIENCES_UPDATER_INITIAL_INTERVAL = ONE_MINUTE;
	private static final int NODE_EXPERIENCES_UPDATER_INITIAL_INTERVAL_ROUNDS = 60;
	private static final int NODE_EXPERIENCES_UPDATER_PLATEAU_INTERVAL = 3 * ONE_HOUR;
	private static final int NODE_EXPERIENCES_UPDATER_BACK_OFF_TIME = 12 * ONE_HOUR;

	private static final int NODE_EXPERIENCES_PRUNE_INTERVAL = 6 * ONE_HOUR;

	private static final int CHECK_CHAIN_SYNC_INTERVAL = 30 * ONE_SECOND;

	private final TimeProvider timeProvider;
	private final HarvestingTask harvestingTask;
	private final List<NemAsyncTimerVisitor> timerVisitors = new ArrayList<>();
	private final List<AsyncTimer> timers = new ArrayList<>();
	private final Executor executor = Executors.newCachedThreadPool();

	/**
	 * Creates a new scheduler.
	 *
	 * @param timeProvider The time provider.
	 * @param harvestingTask The harvesting task.
	 */
	public PeerNetworkScheduler(final TimeProvider timeProvider, final HarvestingTask harvestingTask) {
		this.timeProvider = timeProvider;
		this.harvestingTask = harvestingTask;
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
	 * @param networkBroadcastBuffer The network broadcast buffer.
	 * @param useNetworkTime true if network time should be used.
	 * @param enableAutoIpDetection true if auto IP detection should be enabled.
	 */
	public void addTasks(final PeerNetwork network, final PeerNetworkBroadcastBuffer networkBroadcastBuffer, final boolean useNetworkTime,
			final boolean enableAutoIpDetection) {
		this.addForagingTask(network);

		final NetworkTaskInitializer initializer = new NetworkTaskInitializer(this, network, networkBroadcastBuffer);
		initializer.addDefaultTasks();
		if (useNetworkTime) {
			initializer.addTimeSynchronizationTask();
		}

		if (enableAutoIpDetection) {
			initializer.addAutoIpDetectionTask();
		}
	}

	private void addForagingTask(final PeerNetwork network) {
		final AsyncTimerVisitor timerVisitor = this.createNamedVisitor("FORAGING");
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder()
				.setRecurringFutureSupplier(
						this.runnableToFutureSupplier(() -> this.harvestingTask.harvest(network, this.timeProvider.getCurrentTime())))
				.setInitialDelay(FORAGING_INITIAL_DELAY).setDelayStrategy(new UniformDelayStrategy(FORAGING_INTERVAL))
				.setVisitor(timerVisitor).create();
		this.timers.add(new AsyncTimer(options));
	}

	private static class NetworkTaskInitializer {
		private final PeerNetworkScheduler scheduler;
		private final PeerNetwork network;
		private final PeerNetworkBroadcastBuffer networkBroadcastBuffer;
		private final AsyncTimer refreshTimer;

		public NetworkTaskInitializer(final PeerNetworkScheduler scheduler, final PeerNetwork network,
				final PeerNetworkBroadcastBuffer networkBroadcastBuffer) {
			this.scheduler = scheduler;
			this.network = network;
			this.networkBroadcastBuffer = networkBroadcastBuffer;
			this.refreshTimer = this.addRefreshTask(network);
		}

		public void addDefaultTasks() {
			this.addSimpleTask(() -> this.network.broadcast(NisPeerId.REST_NODE_SIGN_OF_LIFE, this.network.getLocalNode()),
					BROADCAST_INTERVAL, "BROADCAST");
			this.addSimpleTask(this.scheduler.runnableToFutureSupplier(this.network::synchronize), SYNC_INTERVAL, "SYNC");
			this.addSimpleTask(this.scheduler.runnableToFutureSupplier(this.network::pruneInactiveNodes), PRUNE_INACTIVE_NODES_DELAY,
					"PRUNING INACTIVE NODES");
			this.addSimpleTask(this.network::checkChainSynchronization, CHECK_CHAIN_SYNC_INTERVAL, "CHECKING CHAIN SYNCHRONIZATION");
			this.addSimpleTask(this.networkBroadcastBuffer::broadcastAll, BROADCAST_BUFFERED_ENTITIES_INTERVAL,
					"BROADCAST BUFFERED ENTITIES");
			this.addSimpleTask(() -> this.network.updateNodeExperiences(this.scheduler.timeProvider),
					getNodeExperienceUpdaterDelayStrategy(), "UPDATE NODE EXPERIENCES");
			this.addSimpleTask(
					this.scheduler.runnableToFutureSupplier(
							() -> this.network.pruneNodeExperiences(this.scheduler.timeProvider.getCurrentTime())),
					NODE_EXPERIENCES_PRUNE_INTERVAL, "PRUNE NODE EXPERIENCES");
		}

		public void addTimeSynchronizationTask() {
			this.addSimpleTask(() -> this.network.synchronizeTime(this.scheduler.timeProvider), getTimeSynchronizationDelayStrategy(),
					"TIME SYNCHRONIZATION");
		}

		public void addAutoIpDetectionTask() {
			this.addSimpleTask(this.scheduler.runnableToFutureSupplier(this.network::updateLocalNodeEndpoint), AUTO_IP_DETECTION_DELAY,
					"AUTO IP DETECTION");
		}

		private AsyncTimer addRefreshTask(final PeerNetwork network) {
			final AsyncTimerOptionsBuilder builder = new AsyncTimerOptionsBuilder().setRecurringFutureSupplier(network::refresh)
					.setInitialDelay(REFRESH_INITIAL_DELAY).setDelayStrategy(getRefreshDelayStrategy());
			return this.addTask("REFRESH", builder);
		}

		private void addSimpleTask(final Supplier<CompletableFuture<?>> recurringFutureSupplier, final int delay, final String name) {
			this.addSimpleTask(recurringFutureSupplier, new UniformDelayStrategy(delay), name);
		}

		private void addSimpleTask(final Supplier<CompletableFuture<?>> recurringFutureSupplier, final AbstractDelayStrategy delay,
				final String name) {
			final AsyncTimerOptionsBuilder builder = new AsyncTimerOptionsBuilder().setRecurringFutureSupplier(recurringFutureSupplier)
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
		// initially refresh at REFRESH_INITIAL_INTERVAL (1m), gradually increasing to
		// REFRESH_PLATEAU_INTERVAL (5m) over REFRESH_BACK_OFF_TIME (12 hours),
		// and then plateau at that rate forever
		final List<AbstractDelayStrategy> subStrategies = Arrays.asList(
				LinearDelayStrategy.withDuration(REFRESH_INITIAL_INTERVAL, REFRESH_PLATEAU_INTERVAL, REFRESH_BACK_OFF_TIME),
				new UniformDelayStrategy(REFRESH_PLATEAU_INTERVAL));
		return new AggregateDelayStrategy(subStrategies);
	}

	private static AbstractDelayStrategy getTimeSynchronizationDelayStrategy() {
		// initially refresh at TIME_SYNC_INITIAL_INTERVAL (1min), keeping it for TIME_SYNC_INITIAL_INTERVAL_ROUNDS rounds,
		// then gradually increasing to TIME_SYNC_PLATEAU_INTERVAL (1h) over TIME_SYNC_BACK_OFF_TIME (9 hours),
		// and then plateau at that rate forever
		final List<AbstractDelayStrategy> subStrategies = Arrays.asList(
				new UniformDelayStrategy(TIME_SYNC_INITIAL_INTERVAL, TIME_SYNC_INITIAL_INTERVAL_ROUNDS),
				LinearDelayStrategy.withDuration(TIME_SYNC_INITIAL_INTERVAL, TIME_SYNC_PLATEAU_INTERVAL, TIME_SYNC_BACK_OFF_TIME),
				new UniformDelayStrategy(TIME_SYNC_PLATEAU_INTERVAL));
		return new AggregateDelayStrategy(subStrategies);
	}

	private static AbstractDelayStrategy getNodeExperienceUpdaterDelayStrategy() {
		// initially refresh at NODE_EXPERIENCES_UPDATER_INITIAL_INTERVAL (1min),
		// keeping it for NODE_EXPERIENCES_UPDATER_INITIAL_INTERVAL_ROUNDS rounds,
		// then gradually increasing to NODE_EXPERIENCES_UPDATER_PLATEAU_INTERVAL (1h)
		// over NODE_EXPERIENCES_UPDATER_BACK_OFF_TIME (12 hours),
		// and then plateau at that rate forever
		final List<AbstractDelayStrategy> subStrategies = Arrays.asList(
				new UniformDelayStrategy(NODE_EXPERIENCES_UPDATER_INITIAL_INTERVAL, NODE_EXPERIENCES_UPDATER_INITIAL_INTERVAL_ROUNDS),
				LinearDelayStrategy.withDuration(NODE_EXPERIENCES_UPDATER_INITIAL_INTERVAL, NODE_EXPERIENCES_UPDATER_PLATEAU_INTERVAL,
						NODE_EXPERIENCES_UPDATER_BACK_OFF_TIME),
				new UniformDelayStrategy(NODE_EXPERIENCES_UPDATER_PLATEAU_INTERVAL));
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
