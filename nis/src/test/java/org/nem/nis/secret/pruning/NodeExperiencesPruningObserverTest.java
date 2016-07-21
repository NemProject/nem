package org.nem.nis.secret.pruning;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.secret.*;
import org.nem.peer.trust.score.NodeExperiences;

import static org.nem.nis.secret.pruning.AbstractPruningObserverTest.PRUNE_INTERVAL;

public class NodeExperiencesPruningObserverTest {
	private static final int RETENTION_HOURS = 24;

	@Test
	public void timeBasedPruningIsTriggeredAtInitialTime() {
		// Assert:
		this.assertTimeBasedPruning(TimeInstant.ZERO);
	}

	@Test
	public void timeBasedPruningIsTriggeredAtAllTimes() {
		// Assert: state is expected prune timestamp
		final TimeInstant relativeTime1 = TimeInstant.ZERO.addHours(RETENTION_HOURS);
		this.assertTimeBasedPruning(relativeTime1.addSeconds(-1));
		this.assertTimeBasedPruning(relativeTime1);
		this.assertTimeBasedPruning(relativeTime1.addSeconds(1));

		final TimeInstant relativeTime2 = TimeInstant.ZERO.addHours(2 * RETENTION_HOURS);
		this.assertTimeBasedPruning(relativeTime2.addSeconds(-1));
		this.assertTimeBasedPruning(relativeTime2);
		this.assertTimeBasedPruning(relativeTime2.addSeconds(1));

		final TimeInstant relativeTime3 = TimeInstant.ZERO.addHours(3 * RETENTION_HOURS);
		this.assertTimeBasedPruning(relativeTime3.addSeconds(-1));
		this.assertTimeBasedPruning(relativeTime3);
		this.assertTimeBasedPruning(relativeTime3.addSeconds(1));
	}

	@Test
	public void noPruningIsTriggeredWhenNotificationTriggerIsNotExecute() {
		// Assert:
		this.assertNoPruning(120 * PRUNE_INTERVAL + 1, 1, NotificationTrigger.Undo, NotificationType.BlockHarvest);
	}

	@Test
	public void noPruningIsTriggeredWhenNotificationTypeIsNotHarvestReward() {
		// Assert:
		this.assertNoPruning(120 * PRUNE_INTERVAL + 1, 1, NotificationTrigger.Execute, NotificationType.BalanceCredit);
	}

	@Test
	public void noPruningIsTriggeredWhenBlockHeightModuloThreeHundredSixtyIsNotOne() {
		// Assert:
		for (int i = 1; i < 1000; ++i) {
			if (1 != (i % PRUNE_INTERVAL)) {
				this.assertNoPruning(i, 1, NotificationTrigger.Execute, NotificationType.BlockHarvest);
			}
		}
	}

	private void assertNoPruning(
			final long notificationHeight,
			final int notificationTime,
			final NotificationTrigger notificationTrigger,
			final NotificationType notificationType) {
		// Arrange:
		final NodeExperiences experiences = Mockito.mock(NodeExperiences.class);
		final BlockTransactionObserver observer = createObserver(experiences);

		// Act:
		final Notification notification = createAdjustmentNotification(notificationType);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(
				new BlockHeight(notificationHeight),
				new TimeInstant(notificationTime),
				notificationTrigger);
		observer.notify(notification, notificationContext);

		// Assert:
		Mockito.verify(experiences, Mockito.never()).prune(Mockito.any());
	}

	private void assertTimeBasedPruning(final TimeInstant notificationTime) {
		// Arrange:
		final NodeExperiences experiences = Mockito.mock(NodeExperiences.class);
		final BlockTransactionObserver observer = createObserver(experiences);

		// Act:
		final Notification notification = createAdjustmentNotification(NotificationType.BlockHarvest);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(
				BlockHeight.ONE,
				notificationTime,
				NotificationTrigger.Execute);
		observer.notify(notification, notificationContext);

		// Assert:
		Mockito.verify(experiences, Mockito.only()).prune(notificationTime);
	}

	private static Notification createAdjustmentNotification(final NotificationType type) {
		return new BalanceAdjustmentNotification(type, Utils.generateRandomAccount(), Amount.ZERO);
	}

	private static BlockTransactionObserver createObserver(final NodeExperiences experiences) {
		return new NodeExperiencesPruningObserver(experiences);
	}
}
