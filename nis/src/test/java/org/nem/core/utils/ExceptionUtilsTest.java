package org.nem.core.utils;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.*;

public class ExceptionUtilsTest {

    @Test
    public void toUncheckedReturnsAnUncheckedException() throws Exception {
        // Arrange:
        final TestRunner runner = new TestRunner();

        // Act:
        runner.run();

        // Assert:
        Assert.assertThat(runner.getUnhandledException(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(runner.getUnhandledException(), Is.is(IllegalStateException.class));
    }

    @Test
    public void toUncheckedSetsThreadInterruptFlag() throws Exception {
        // Arrange:
        final TestRunner runner = new TestRunner();

        // Act:
        runner.run();

        // Assert:
        Assert.assertThat(runner.isInterruptedPreRun(), IsEqual.equalTo(false));
        Assert.assertThat(runner.isInterruptedPostRun(), IsEqual.equalTo(true));
    }

    private class TestRunner {

        private final Thread blockingThread;

        private boolean isInterruptedPreRun;
        private boolean isInterruptedPostRun;
        private Throwable unhandledException;

        public TestRunner() {
            this.blockingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    isInterruptedPreRun = Thread.currentThread().isInterrupted();

                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        throw ExceptionUtils.toUnchecked(e);
                    }
                    finally {
                        isInterruptedPostRun = Thread.currentThread().isInterrupted();
                    }
                }
            });

            this.blockingThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    unhandledException = e;
                }
            });
        }

        public boolean isInterruptedPreRun() { return this.isInterruptedPreRun; }
        public boolean isInterruptedPostRun() { return this.isInterruptedPostRun; }
        public Throwable getUnhandledException() { return this.unhandledException; }

        public void run() throws InterruptedException {
            this.blockingThread.start();
            Thread.sleep(10);

            this.blockingThread.interrupt();
            this.blockingThread.join();
        }
    }
}
