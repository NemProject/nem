package org.nem.nis.boot;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.*;
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
			scheduler.addTasks(Mockito.mock(PeerNetwork.class), Mockito.mock(BlockChain.class));
			final List<String> taskNames = scheduler.getVisitors().stream()
					.map(NisAsyncTimerVisitor::getTimerName)
					.collect(Collectors.toList());

			// Assert:
			final List<String> expectedTaskNames = Arrays.asList(
					"BROADCAST",
					"FORAGING",
					"PRUNING INACTIVE NODES",
					"REFRESH",
					"SYNC",
					"UPDATING LOCAL NODE ENDPOINT");
			Assert.assertThat(taskNames, IsEquivalent.equivalentTo(expectedTaskNames));
		}
	}
}