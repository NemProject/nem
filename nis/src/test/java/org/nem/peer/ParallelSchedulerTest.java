package org.nem.peer;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.*;

import java.util.*;
import java.util.concurrent.RejectedExecutionException;

public class ParallelSchedulerTest {

    @Test
    public void tasksAreExecutedOnDifferentThreads() {
        // Arrange:
        final SleepAction action = new SleepAction(5);
        final ParallelScheduler<Integer> scheduler = new ParallelScheduler<>(2, action);

        // Act:
        scheduler.push(Arrays.asList(1, 2));
        scheduler.block();

        // Assert:
        final List<Long> threadIds = action.getThreadIds();
        Assert.assertThat(threadIds.size(), IsEqual.equalTo(2));
        Assert.assertThat(threadIds.get(1), IsNot.not(IsEqual.equalTo(threadIds.get(0))));
    }

    @Test
    public void tasksCanBePushedMultipleTimes() {
        // Arrange:
        final SleepAction action = new SleepAction(5);
        final ParallelScheduler<Integer> scheduler = new ParallelScheduler<>(2, action);

        // Act:
        scheduler.push(Arrays.asList(1, 2));
        scheduler.push(Arrays.asList(3, 4));
        scheduler.block();

        // Assert:
        Assert.assertThat(action.getThreadIds().size(), IsEqual.equalTo(4));
    }

    @Test
    public void blockBlocksUntilAllTasksAreComplete() {
        // Arrange:
        final SleepAction action = new SleepAction(100);
        final ParallelScheduler<Integer> scheduler = new ParallelScheduler<>(2, action);

        // Act:
        scheduler.push(Arrays.asList(1, 2));
        scheduler.block();

        // Assert:
        Assert.assertThat(action.getThreadIds().size(), IsEqual.equalTo(2));
    }

    // TODO: consider throwing a nicer exception
    @Test(expected = RejectedExecutionException.class)
    public void pushCannotBeCalledAfterBlock() {
        // Arrange:
        final SleepAction action = new SleepAction(5);
        final ParallelScheduler<Integer> scheduler = new ParallelScheduler<>(2, action);

        // Act:
        scheduler.block();
        scheduler.push(Arrays.asList(1, 2));

        // Assert:
        Assert.assertThat(action.getThreadIds().size(), IsEqual.equalTo(2));
    }

    private static class SleepAction implements ParallelScheduler.Action<Integer> {

        final int sleepMillis;
        final List<Long> threadIds;

        public SleepAction(final int sleepMillis) {
            this.sleepMillis = sleepMillis;
            this.threadIds = Collections.synchronizedList(new ArrayList<Long>());
        }

        public List<Long> getThreadIds() { return this.threadIds; }

        @Override
        public void execute(final Integer element) {
            try {
                this.threadIds.add(Thread.currentThread().getId());
                Thread.sleep(this.sleepMillis);
            }
            catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
