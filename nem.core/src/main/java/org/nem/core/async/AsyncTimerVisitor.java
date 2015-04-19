package org.nem.core.async;

/**
 * A visitor that is called by the AsyncTimer.
 */
public interface AsyncTimerVisitor {

	/**
	 * Called to indicate that a user operation is starting.
	 */
	void notifyOperationStart();

	/**
	 * Called to indicate that a user operation has completed (successfully).
	 */
	void notifyOperationComplete();

	/**
	 * Called to indicate that a user operation has completed (exceptionally).
	 *
	 * @param ex The exception.
	 */
	void notifyOperationCompleteExceptionally(final Throwable ex);

	/**
	 * Called to indicate that the timer will delay for the specified interval before starting
	 * the next operation.
	 *
	 * @param delay The delay in milliseconds.
	 */
	void notifyDelay(int delay);

	/**
	 * Called to indicate that the timer has stopped.
	 */
	void notifyStop();
}
