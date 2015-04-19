package org.nem.core.utils;

import org.hamcrest.core.*;
import org.junit.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class ExceptionUtilsTest {

	//region propagate

	@Test
	public void propagateReturnsResultOnSuccess() {
		// Act:
		final int result = ExceptionUtils.propagate(() -> 7);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(7));
	}

	@Test(expected = IllegalArgumentException.class)
	public void propagateAllowsRuntimeExceptionsToPropagate() {
		// Act:
		ExceptionUtils.propagate(() -> { throw new IllegalArgumentException(); });
	}

	@Test(expected = RuntimeException.class)
	public void propagateWrapsCheckedExceptionsInRuntimeExceptionByDefault() {
		// Act:
		ExceptionUtils.propagate(() -> { throw new IOException(); });
	}

	@Test(expected = IllegalArgumentException.class)
	public void propagateCanWrapCheckedExceptionsInCustomRuntimeException() {
		// Act:
		ExceptionUtils.propagate(
				() -> { throw new IOException(); },
				IllegalArgumentException::new);
	}

	@Test(expected = IllegalArgumentException.class)
	public void propagateUnwrapsUncheckedExecutionExceptions() {
		// Act:
		ExceptionUtils.propagate(() -> { throw new MockExecutionException(new IllegalArgumentException()); });
	}

	@Test(expected = RuntimeException.class)
	public void propagateWrapsCheckedExecutionExceptionsInRuntimeExceptionByDefault() {
		// Act:
		ExceptionUtils.propagate(() -> { throw new MockExecutionException(new IOException()); });
	}

	@Test(expected = IllegalArgumentException.class)
	public void propagateCanWrapCheckedExecutionExceptionsInCustomRuntimeException() {
		// Act:
		ExceptionUtils.propagate(
				() -> { throw new MockExecutionException(new IOException()); },
				IllegalArgumentException::new);
	}

	@Test
	public void propagateMapsInterruptedExceptionToIllegalStateException() throws InterruptedException {
		// Arrange:
		final InterruptedExceptionTestRunner runner = new InterruptedExceptionTestRunner(() ->
				ExceptionUtils.propagate(() -> {
					Thread.sleep(1000);
					return null;
				}));

		// Act:
		runner.run();

		// Assert:
		Assert.assertThat(runner.getUnhandledException(), IsNull.notNullValue());
		Assert.assertThat(runner.getUnhandledException(), IsInstanceOf.instanceOf(IllegalStateException.class));
	}

	@Test
	public void propagateSetsThreadInterruptFlagWhenMappingInterruptedException() throws InterruptedException {
		// Arrange:
		final InterruptedExceptionTestRunner runner = new InterruptedExceptionTestRunner(() ->
				ExceptionUtils.propagate(() -> {
					Thread.sleep(1000);
					return null;
				}));

		// Act:
		runner.run();

		// Assert:
		Assert.assertThat(runner.isInterruptedPreRun(), IsEqual.equalTo(false));
		Assert.assertThat(runner.isInterruptedPostRun(), IsEqual.equalTo(true));
	}

	//endregion

	//region propagateVoid

	@Test
	public void propagateVoidDoesNotThrowExceptionOnSuccess() {
		// Act:
		ExceptionUtils.propagateVoid(() -> { });

		// Assert: (no exception)
	}

	@Test(expected = IllegalArgumentException.class)
	public void propagateVoidAllowsRuntimeExceptionsToPropagate() {
		// Act:
		ExceptionUtils.propagateVoid(() -> { throw new IllegalArgumentException(); });
	}

	@Test(expected = RuntimeException.class)
	public void propagateVoidWrapsCheckedExceptionsInRuntimeExceptionByDefault() {
		// Act:
		ExceptionUtils.propagateVoid(() -> { throw new IOException(); });
	}

	@Test(expected = IllegalArgumentException.class)
	public void propagateVoidCanWrapCheckedExceptionsInCustomRuntimeException() {
		// Act:
		ExceptionUtils.propagateVoid(
				() -> { throw new IOException(); },
				IllegalArgumentException::new);
	}

	@Test(expected = IllegalArgumentException.class)
	public void propagateVoidUnwrapsUncheckedExecutionExceptions() {
		// Act:
		ExceptionUtils.propagateVoid(() -> { throw new MockExecutionException(new IllegalArgumentException()); });
	}

	@Test(expected = RuntimeException.class)
	public void propagateVoidWrapsCheckedExecutionExceptionsInRuntimeExceptionByDefault() {
		// Act:
		ExceptionUtils.propagateVoid(() -> { throw new MockExecutionException(new IOException()); });
	}

	@Test(expected = IllegalArgumentException.class)
	public void propagateVoidCanWrapCheckedExecutionExceptionsInCustomRuntimeException() {
		// Act:
		ExceptionUtils.propagateVoid(
				() -> { throw new MockExecutionException(new IOException()); },
				IllegalArgumentException::new);
	}

	@Test
	public void propagateVoidMapsInterruptedExceptionToIllegalStateException() throws InterruptedException {
		// Arrange:
		final InterruptedExceptionTestRunner runner = new InterruptedExceptionTestRunner(() -> {
			ExceptionUtils.propagateVoid(() -> Thread.sleep(1000));
			return null;
		});

		// Act:
		runner.run();

		// Assert:
		Assert.assertThat(runner.getUnhandledException(), IsNull.notNullValue());
		Assert.assertThat(runner.getUnhandledException(), IsInstanceOf.instanceOf(IllegalStateException.class));
	}

	@Test
	public void propagateVoidSetsThreadInterruptFlagWhenMappingInterruptedException() throws InterruptedException {
		// Arrange:
		final InterruptedExceptionTestRunner runner = new InterruptedExceptionTestRunner(() -> {
			ExceptionUtils.propagateVoid(() -> Thread.sleep(1000));
			return null;
		});

		// Act:
		runner.run();

		// Assert:
		Assert.assertThat(runner.isInterruptedPreRun(), IsEqual.equalTo(false));
		Assert.assertThat(runner.isInterruptedPostRun(), IsEqual.equalTo(true));
	}

	//endregion

	private class InterruptedExceptionTestRunner {

		private final Thread blockingThread;

		private boolean isInterruptedPreRun;
		private boolean isInterruptedPostRun;
		private Throwable unhandledException;

		public InterruptedExceptionTestRunner(final Supplier<Void> supplier) {
			this.blockingThread = new Thread(() -> {
				this.isInterruptedPreRun = Thread.currentThread().isInterrupted();

				try {
					supplier.get();
				} finally {
					this.isInterruptedPostRun = Thread.currentThread().isInterrupted();
				}
			});

			this.blockingThread.setUncaughtExceptionHandler((t, e) -> this.unhandledException = e);
		}

		public boolean isInterruptedPreRun() {
			return this.isInterruptedPreRun;
		}

		public boolean isInterruptedPostRun() {
			return this.isInterruptedPostRun;
		}

		public Throwable getUnhandledException() {
			return this.unhandledException;
		}

		public void run() throws InterruptedException {
			this.blockingThread.start();
			Thread.sleep(10);

			this.blockingThread.interrupt();
			this.blockingThread.join();
		}
	}

	private class MockExecutionException extends ExecutionException {

		public MockExecutionException(final Throwable cause) {
			super(cause);
		}
	}
}
