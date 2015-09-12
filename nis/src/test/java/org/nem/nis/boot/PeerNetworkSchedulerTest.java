package org.nem.nis.boot;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.harvesting.HarvestingTask;
import org.nem.peer.PeerNetwork;

import java.util.*;
import java.util.stream.Collectors;

public class PeerNetworkSchedulerTest {

	@Test
	public void initiallyNoTasksAreStarted() {
		// Act:
		try (final PeerNetworkScheduler scheduler = createScheduler()) {
			// Assert:
			Assert.assertThat(scheduler.getVisitors().size(), IsEqual.equalTo(0));
		}
	}

	@Test
	public void addTasksAddsAllNisTasks() {
		// Arrange:
		try (final PeerNetworkScheduler scheduler = createScheduler()) {
			// Act:
			scheduler.addTasks(Mockito.mock(PeerNetwork.class), true, true);
			final List<String> taskNames = getTaskNames(scheduler);

			// Assert:
			final List<String> expectedTaskNames = Arrays.asList(
					"BROADCAST",
					"FORAGING",
					"PRUNING INACTIVE NODES",
					"REFRESH",
					"SYNC",
					"AUTO IP DETECTION",
					"TIME SYNCHRONIZATION",
					"CHECKING CHAIN SYNCHRONIZATION",
					"BROADCAST BUFFERED ENTITIES");
			Assert.assertThat(taskNames, IsEquivalent.equivalentTo(expectedTaskNames));
		}
	}

	@Test
	public void timeSynchronizationTaskCanBeExcluded() {
		// Arrange:
		try (final PeerNetworkScheduler scheduler = createScheduler()) {
			// Act:
			scheduler.addTasks(Mockito.mock(PeerNetwork.class), false, true);
			final List<String> taskNames = getTaskNames(scheduler);

			// Assert:
			Assert.assertThat(!taskNames.contains("TIME SYNCHRONIZATION"), IsEqual.equalTo(true));
		}
	}

	@Test
	public void autoIpDetectionTaskCanBeExcluded() {
		// Arrange:
		try (final PeerNetworkScheduler scheduler = createScheduler()) {
			// Act:
			scheduler.addTasks(Mockito.mock(PeerNetwork.class), true, false);
			final List<String> taskNames = getTaskNames(scheduler);

			// Assert:
			Assert.assertThat(!taskNames.contains("AUTO IP DETECTION"), IsEqual.equalTo(true));
		}
	}

	@Test
	public void allOptionalTasksCanBeExcluded() {
		// Arrange:
		try (final PeerNetworkScheduler scheduler = createScheduler()) {
			// Act:
			scheduler.addTasks(Mockito.mock(PeerNetwork.class), false, false);
			final List<String> taskNames = getTaskNames(scheduler);

			// Assert:
			final List<String> expectedTaskNames = Arrays.asList(
					"BROADCAST",
					"FORAGING",
					"PRUNING INACTIVE NODES",
					"REFRESH",
					"SYNC",
					"CHECKING CHAIN SYNCHRONIZATION",
					"BROADCAST BUFFERED ENTITIES");
			Assert.assertThat(taskNames, IsEquivalent.equivalentTo(expectedTaskNames));
		}
	}

	private static List<String> getTaskNames(final PeerNetworkScheduler scheduler) {
		return scheduler.getVisitors().stream()
				.map(NemAsyncTimerVisitor::getTimerName)
				.collect(Collectors.toList());
	}

	private static PeerNetworkScheduler createScheduler() {
		return new PeerNetworkScheduler(
				Mockito.mock(TimeProvider.class),
				Mockito.mock(HarvestingTask.class));
	}
}