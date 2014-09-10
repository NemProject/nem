package org.nem.nis.boot;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.BlockChain;
import org.nem.peer.PeerNetwork;

import java.util.*;
import java.util.stream.Collectors;

public class PeerNetworkSchedulerTest {

	@Test
	public void initiallyNoTasksAreStarted() {
		// Act:
		try (final PeerNetworkScheduler scheduler = new PeerNetworkScheduler(Mockito.mock(TimeProvider.class))) {
			// Assert:
			Assert.assertThat(scheduler.getVisitors().size(), IsEqual.equalTo(0));
		}
	}

	@Test
	public void addTasksAddsAllNisTasks() {
		// Arrange:
		try (final PeerNetworkScheduler scheduler = new PeerNetworkScheduler(Mockito.mock(TimeProvider.class))) {
			// Act:
			scheduler.addTasks(Mockito.mock(PeerNetwork.class), Mockito.mock(BlockChain.class), true);
			final List<String> taskNames = scheduler.getVisitors().stream()
					.map(NemAsyncTimerVisitor::getTimerName)
					.collect(Collectors.toList());

			// Assert:
			final List<String> expectedTaskNames = Arrays.asList(
					"BROADCAST",
					"FORAGING",
					"PRUNING INACTIVE NODES",
					"REFRESH",
					"SYNC",
					"UPDATING LOCAL NODE ENDPOINT",
					"TIME SYNCHRONIZATION");
			Assert.assertThat(taskNames, IsEquivalent.equivalentTo(expectedTaskNames));
		}
	}

	@Test
	public void timeSynchronizationTaskCanBeExcluded() {
		// Arrange:
		try (final PeerNetworkScheduler scheduler = new PeerNetworkScheduler(Mockito.mock(TimeProvider.class))) {
			// Act:
			scheduler.addTasks(Mockito.mock(PeerNetwork.class), Mockito.mock(BlockChain.class), false);
			final List<String> taskNames = scheduler.getVisitors().stream()
					.map(NemAsyncTimerVisitor::getTimerName)
					.collect(Collectors.toList());

			// Assert:
			Assert.assertThat(!taskNames.contains("TIME SYNCHRONIZATION"), IsEqual.equalTo(true));
		}
	}
}