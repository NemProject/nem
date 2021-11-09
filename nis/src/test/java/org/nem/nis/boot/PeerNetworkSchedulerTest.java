package org.nem.nis.boot;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.harvesting.HarvestingTask;
import org.nem.peer.*;

import java.util.*;
import java.util.stream.Collectors;

public class PeerNetworkSchedulerTest {

	@Test
	public void initiallyNoTasksAreStarted() {
		// Act:
		try (final PeerNetworkScheduler scheduler = createScheduler()) {
			// Assert:
			MatcherAssert.assertThat(scheduler.getVisitors().size(), IsEqual.equalTo(0));
		}
	}

	@Test
	public void addTasksAddsAllNisTasks() {
		// Arrange:
		try (final PeerNetworkScheduler scheduler = createScheduler()) {
			// Act:
			addTasks(scheduler, true, true);
			final List<String> taskNames = getTaskNames(scheduler);

			// Assert:
			final List<String> expectedTaskNames = Arrays.asList("BROADCAST", "FORAGING", "PRUNING INACTIVE NODES", "REFRESH", "SYNC",
					"AUTO IP DETECTION", "TIME SYNCHRONIZATION", "CHECKING CHAIN SYNCHRONIZATION", "BROADCAST BUFFERED ENTITIES",
					"UPDATE NODE EXPERIENCES", "PRUNE NODE EXPERIENCES");
			MatcherAssert.assertThat(taskNames, IsEquivalent.equivalentTo(expectedTaskNames));
		}
	}

	@Test
	public void timeSynchronizationTaskCanBeExcluded() {
		// Arrange:
		try (final PeerNetworkScheduler scheduler = createScheduler()) {
			// Act:
			addTasks(scheduler, false, true);
			final List<String> taskNames = getTaskNames(scheduler);

			// Assert:
			MatcherAssert.assertThat(!taskNames.contains("TIME SYNCHRONIZATION"), IsEqual.equalTo(true));
		}
	}

	@Test
	public void autoIpDetectionTaskCanBeExcluded() {
		// Arrange:
		try (final PeerNetworkScheduler scheduler = createScheduler()) {
			// Act:
			addTasks(scheduler, true, false);
			final List<String> taskNames = getTaskNames(scheduler);

			// Assert:
			MatcherAssert.assertThat(!taskNames.contains("AUTO IP DETECTION"), IsEqual.equalTo(true));
		}
	}

	@Test
	public void allOptionalTasksCanBeExcluded() {
		// Arrange:
		try (final PeerNetworkScheduler scheduler = createScheduler()) {
			// Act:
			addTasks(scheduler, false, false);
			final List<String> taskNames = getTaskNames(scheduler);

			// Assert:
			final List<String> expectedTaskNames = Arrays.asList("BROADCAST", "FORAGING", "PRUNING INACTIVE NODES", "REFRESH", "SYNC",
					"CHECKING CHAIN SYNCHRONIZATION", "BROADCAST BUFFERED ENTITIES", "UPDATE NODE EXPERIENCES", "PRUNE NODE EXPERIENCES");
			MatcherAssert.assertThat(taskNames, IsEquivalent.equivalentTo(expectedTaskNames));
		}
	}

	private static List<String> getTaskNames(final PeerNetworkScheduler scheduler) {
		return scheduler.getVisitors().stream().map(NemAsyncTimerVisitor::getTimerName).collect(Collectors.toList());
	}

	private static PeerNetworkScheduler createScheduler() {
		return new PeerNetworkScheduler(Mockito.mock(TimeProvider.class), Mockito.mock(HarvestingTask.class));
	}

	private static void addTasks(final PeerNetworkScheduler scheduler, final boolean useNetworkTime, final boolean enableAutoIpDetection) {
		scheduler.addTasks(Mockito.mock(PeerNetwork.class), Mockito.mock(PeerNetworkBroadcastBuffer.class), useNetworkTime,
				enableAutoIpDetection);
	}
}
