package org.nem.peer;

import org.hamcrest.core.*;
import org.junit.*;

import java.util.*;

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
    public void pushIsNonBlocking() {
        // Arrange:
        final BlockAction action = new BlockAction();
        final ParallelScheduler<Integer> scheduler = new ParallelScheduler<>(2, action);

        // Act:
        scheduler.push(Arrays.asList(1, 2));

        // Assert:
        final List<Long> threadIds = action.getThreadIds();
        Assert.assertThat(threadIds.size(), IsEqual.equalTo(0));

        // Cleanup:
        action.unblock();
        scheduler.block();
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

    @Test(expected = IllegalStateException.class)
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

    //region BlockAction

    private static class BlockAction implements ParallelScheduler.Action<Integer> {

        private final Object monitor;
        private final List<Long> threadIds;
        private boolean canBlock;

        public BlockAction() {
            this.monitor = new Object();
            this.threadIds = Collections.synchronizedList(new ArrayList<Long>());
            this.canBlock = true;
        }

        public List<Long> getThreadIds() { return this.threadIds; }

        public void unblock() {
            synchronized (monitor) {
                this.canBlock = false;
                this.monitor.notifyAll();
            }
        }

        @Override
        public void execute(final Integer element) {
            try {
                synchronized (monitor) {
                    if (this.canBlock)
                        this.monitor.wait();
                }

                this.threadIds.add(Thread.currentThread().getId());
            }
            catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    //endregion

    //region SleepAction

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
                Thread.sleep(this.sleepMillis);
                this.threadIds.add(Thread.currentThread().getId());
            }
            catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    //endregion
}
