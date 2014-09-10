package org.nem.core.async;

import org.nem.core.serialization.*;
import org.nem.core.time.*;

import java.util.logging.*;

/**
 * An async timer visitor implementation used by NIS.
 */
public class NemAsyncTimerVisitor implements AsyncTimerVisitor, SerializableEntity {
	private static final Logger LOGGER = Logger.getLogger(NemAsyncTimerVisitor.class.getName());

	private final String timerName;
	private final TimeProvider timeProvider;

	private int numExecutions;
	private int numSuccesses;
	private int numFailures;

	private TimeInstant lastOperationStartTime = TimeInstant.ZERO;
	private int lastOperationTime;
	private int lastDelayTime;
	private int totalTime;

	/**
	 * Creates a new visitor.
	 *
	 * @param timerName The friendly name of the timer.
	 * @param timeProvider The time provider.
	 */
	public NemAsyncTimerVisitor(final String timerName, final TimeProvider timeProvider) {
		this.timerName = timerName;
		this.timeProvider = timeProvider;
	}

	//region getters

	/**
	 * Gets the friendly name of the timer.
	 *
	 * @return The friendly name of the timer.
	 */
	public String getTimerName() {
		return this.timerName;
	}

	/**
	 * Gets the number of executions.
	 *
	 * @return The number of executions.
	 */
	public int getNumExecutions() {
		return this.numExecutions;
	}

	/**
	 * Gets the number of successful completions.
	 *
	 * @return The number of successful completions.
	 */
	public int getNumSuccesses() {
		return this.numSuccesses;
	}

	/**
	 * Gets the number of exceptional completions.
	 *
	 * @return The number of exceptional completions.
	 */
	public int getNumFailures() {
		return this.numFailures;
	}

	/**
	 * Gets the start time of the last operation.
	 *
	 * @return The start time of the last operation.
	 */
	public TimeInstant getLastOperationStartTime() {
		return this.lastOperationStartTime;
	}

	/**
	 * Gets the total time of the last completed operation.
	 *
	 * @return The total time of the last completed operation.
	 */
	public int getLastOperationTime() {
		return this.lastOperationTime;
	}

	/**
	 * Gets the last delay time.
	 *
	 * @return The last delay time.
	 */
	public int getLastDelayTime() {
		return this.lastDelayTime;
	}

	/**
	 * Gets the average time of all completed operations.
	 *
	 * @return The average time of all completed operations.
	 */
	public int getAverageOperationTime() {
		return this.getNumCompletions() > 0 ? this.totalTime / this.getNumCompletions() : 0;
	}

	/**
	 * Gets the a value indicating whether or not the timer is currently executing.
	 *
	 * @return true if the timer is currently executing.
	 */
	public boolean isExecuting() {
		return this.numExecutions > this.getNumCompletions();
	}

	private int getNumCompletions() {
		return this.numSuccesses + this.numFailures;
	}

	//endregion

	//region AsyncTimerVisitor

	@Override
	public void notifyOperationStart() {
		this.log("executing");

		this.lastOperationStartTime = this.timeProvider.getCurrentTime();
		++this.numExecutions;
	}

	@Override
	public void notifyOperationComplete() {
		this.handleOperationCompletion();
		++this.numSuccesses;
	}

	@Override
	public void notifyOperationCompleteExceptionally(final Throwable ex) {
		LOGGER.log(
				Level.WARNING,
				String.format("Timer %s raised exception: %s", this.getTimerName(), ex.getMessage()),
				ex);

		this.handleOperationCompletion();
		++this.numFailures;
	}

	@Override
	public void notifyDelay(final int delay) {
		this.log(String.format("sleeping for %d ms", delay));
		this.lastDelayTime = delay;
	}

	@Override
	public void notifyStop() {
		this.log("stopping");
	}

	private void handleOperationCompletion() {
		final TimeInstant stopTime = this.timeProvider.getCurrentTime();
		this.lastOperationTime = stopTime.subtract(this.lastOperationStartTime);
		this.totalTime += this.lastOperationTime;
	}

	private void log(final String message) {
		LOGGER.fine(String.format(
				"[%d] Timer %s: %s",
				Thread.currentThread().getId(),
				this.timerName,
				message));
	}

	//endregion

	//region SerializableEntity

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeString("name", this.getTimerName());
		serializer.writeInt("executions", this.getNumExecutions());
		serializer.writeInt("successes", this.getNumSuccesses());
		serializer.writeInt("failures", this.getNumFailures());
		serializer.writeInt("last-delay-time", this.getLastDelayTime());
		TimeInstant.writeTo(serializer, "last-operation-start-time", this.getLastOperationStartTime());
		serializer.writeInt("last-operation-time", this.getLastOperationTime());
		serializer.writeInt("average-operation-time", this.getAverageOperationTime());
		serializer.writeInt("is-executing", this.isExecuting() ? 1 : 0);
	}

	//endregion
}
