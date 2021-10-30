package org.nem.core.async;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.core.time.*;

public class NemAsyncTimerVisitorTest {

	@Test
	public void visitorIsInitializedWithDefaultValues() {
		// Arrange:
		final NemAsyncTimerVisitor visitor = new NemAsyncTimerVisitor("timer a", Mockito.mock(TimeProvider.class));

		// Assert:
		assertDefaultValuesWithLastDelayTime(visitor, 0);
	}

	@Test
	public void notifyOperationStartUpdatesVisitorStateCorrectly() {
		// Arrange:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(71, 101);
		final NemAsyncTimerVisitor visitor = new NemAsyncTimerVisitor("timer a", timeProvider);

		// Act:
		visitor.notifyOperationStart();

		// Assert:
		MatcherAssert.assertThat(visitor.getTimerName(), IsEqual.equalTo("timer a"));
		MatcherAssert.assertThat(visitor.getNumExecutions(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(visitor.getNumSuccesses(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getNumFailures(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getLastOperationStartTime(), IsEqual.equalTo(new TimeInstant(71)));
		MatcherAssert.assertThat(visitor.getLastOperationTime(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getLastDelayTime(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getAverageOperationTime(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.isExecuting(), IsEqual.equalTo(true));
	}

	@Test
	public void notifyOperationCompleteUpdatesVisitorStateCorrectly() {
		// Arrange:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(71, 101);
		final NemAsyncTimerVisitor visitor = new NemAsyncTimerVisitor("timer a", timeProvider);

		// Act:
		visitor.notifyOperationStart();
		visitor.notifyOperationComplete();

		// Assert:
		MatcherAssert.assertThat(visitor.getTimerName(), IsEqual.equalTo("timer a"));
		MatcherAssert.assertThat(visitor.getNumExecutions(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(visitor.getNumSuccesses(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(visitor.getNumFailures(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getLastOperationStartTime(), IsEqual.equalTo(new TimeInstant(71)));
		MatcherAssert.assertThat(visitor.getLastOperationTime(), IsEqual.equalTo(30));
		MatcherAssert.assertThat(visitor.getLastDelayTime(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getAverageOperationTime(), IsEqual.equalTo(30));
		MatcherAssert.assertThat(visitor.isExecuting(), IsEqual.equalTo(false));
	}

	@Test
	public void notifyOperationCompleteExceptionallyUpdatesVisitorStateCorrectly() {
		// Arrange:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(71, 101);
		final NemAsyncTimerVisitor visitor = new NemAsyncTimerVisitor("timer a", timeProvider);

		// Act:
		visitor.notifyOperationStart();
		visitor.notifyOperationCompleteExceptionally(new RuntimeException("test exception"));

		// Assert:
		MatcherAssert.assertThat(visitor.getTimerName(), IsEqual.equalTo("timer a"));
		MatcherAssert.assertThat(visitor.getNumExecutions(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(visitor.getNumSuccesses(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getNumFailures(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(visitor.getLastOperationStartTime(), IsEqual.equalTo(new TimeInstant(71)));
		MatcherAssert.assertThat(visitor.getLastOperationTime(), IsEqual.equalTo(30));
		MatcherAssert.assertThat(visitor.getLastDelayTime(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getAverageOperationTime(), IsEqual.equalTo(30));
		MatcherAssert.assertThat(visitor.isExecuting(), IsEqual.equalTo(false));
	}

	@Test
	public void averageOperationTimeIsCalculatedCorrectlyWhenThereAreMultipleCompletions() {
		// Arrange:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(71, 101, 101, 121, 121, 131);
		final NemAsyncTimerVisitor visitor = new NemAsyncTimerVisitor("timer a", timeProvider);

		// Act:
		visitor.notifyOperationStart();
		visitor.notifyOperationComplete();
		visitor.notifyOperationStart();
		visitor.notifyOperationCompleteExceptionally(new RuntimeException("test exception"));
		visitor.notifyOperationStart();
		visitor.notifyOperationComplete();

		// Assert:
		MatcherAssert.assertThat(visitor.getLastOperationTime(), IsEqual.equalTo(10));
		MatcherAssert.assertThat(visitor.getAverageOperationTime(), IsEqual.equalTo(20));
	}

	@Test
	public void notifyDelayUpdatesLastDelayTime() {
		// Arrange:
		final NemAsyncTimerVisitor visitor = new NemAsyncTimerVisitor("timer a", Mockito.mock(TimeProvider.class));

		// Act:
		visitor.notifyDelay(12);
		visitor.notifyDelay(23);

		// Assert:
		assertDefaultValuesWithLastDelayTime(visitor, 23);
	}

	@Test
	public void notifyStopUpdatesNothing() {
		// Arrange:
		final NemAsyncTimerVisitor visitor = new NemAsyncTimerVisitor("timer a", Mockito.mock(TimeProvider.class));

		// Act:
		visitor.notifyStop();

		// Assert:
		assertDefaultValuesWithLastDelayTime(visitor, 0);
	}

	@Test
	public void visitorCanBeSerializedWhileExecuting() {
		// Assert:
		assertVisitorSerialization(true);
	}

	@Test
	public void visitorCanBeSerializedWhileNotExecuting() {
		// Assert:
		assertVisitorSerialization(false);
	}

	private static void assertVisitorSerialization(final boolean isExecuting) {
		// Arrange:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(71, 101, 101, 121, 121, 131, 142);
		final NemAsyncTimerVisitor visitor = new NemAsyncTimerVisitor("timer a", timeProvider);

		visitor.notifyOperationStart();
		visitor.notifyOperationComplete();
		visitor.notifyOperationStart();
		visitor.notifyOperationCompleteExceptionally(new RuntimeException("test exception"));
		visitor.notifyOperationStart();
		visitor.notifyOperationComplete();
		visitor.notifyDelay(42);

		if (isExecuting) {
			visitor.notifyOperationStart();
		}

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(visitor);

		// Assert:
		final int executingAdjustment = isExecuting ? 1 : 0;
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(9));
		MatcherAssert.assertThat(jsonObject.get("name"), IsEqual.equalTo("timer a"));
		MatcherAssert.assertThat(jsonObject.get("executions"), IsEqual.equalTo(3 + executingAdjustment));
		MatcherAssert.assertThat(jsonObject.get("successes"), IsEqual.equalTo(2));
		MatcherAssert.assertThat(jsonObject.get("failures"), IsEqual.equalTo(1));
		MatcherAssert.assertThat(jsonObject.get("last-delay-time"), IsEqual.equalTo(42));
		MatcherAssert.assertThat(jsonObject.get("last-operation-start-time"), IsEqual.equalTo(isExecuting ? 142 : 121));
		MatcherAssert.assertThat(jsonObject.get("last-operation-time"), IsEqual.equalTo(10));
		MatcherAssert.assertThat(jsonObject.get("average-operation-time"), IsEqual.equalTo(20));
		MatcherAssert.assertThat(jsonObject.get("is-executing"), IsEqual.equalTo(executingAdjustment));
	}

	private static void assertDefaultValuesWithLastDelayTime(final NemAsyncTimerVisitor visitor, final int expectedLastDelayTime) {
		// Assert:
		MatcherAssert.assertThat(visitor.getTimerName(), IsEqual.equalTo("timer a"));
		MatcherAssert.assertThat(visitor.getNumExecutions(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getNumSuccesses(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getNumFailures(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getLastOperationStartTime(), IsEqual.equalTo(TimeInstant.ZERO));
		MatcherAssert.assertThat(visitor.getLastOperationTime(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.getLastDelayTime(), IsEqual.equalTo(expectedLastDelayTime));
		MatcherAssert.assertThat(visitor.getAverageOperationTime(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(visitor.isExecuting(), IsEqual.equalTo(false));
	}
}
