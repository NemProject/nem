package org.nem.core.async;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AsyncTimerOptionsBuilderTest {
	private static final int TIME_UNIT = 60;
	private static final int TIME_HALF_UNIT = TIME_UNIT / 2;

	@Test
	public void canCreateOptions() {
		// Arrange:
		final Supplier<CompletableFuture<?>> recurringFutureSupplier = () -> CompletableFuture.completedFuture(false);
		final CompletableFuture<?> trigger = CompletableFuture.completedFuture(false);
		final AbstractDelayStrategy delayStrategy = Mockito.mock(AbstractDelayStrategy.class);
		final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);

		// Act:
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder()
				.setRecurringFutureSupplier(recurringFutureSupplier)
				.setTrigger(trigger)
				.setDelayStrategy(delayStrategy)
				.setVisitor(visitor)
				.create();

		// Assert:
		Assert.assertThat(options.getRecurringFutureSupplier(), IsSame.sameInstance(recurringFutureSupplier));
		Assert.assertThat(options.getInitialTrigger(), IsSame.sameInstance(trigger));
		Assert.assertThat(options.getDelayStrategy(), IsSame.sameInstance(delayStrategy));
		Assert.assertThat(options.getVisitor(), IsSame.sameInstance(visitor));
	}

	@Test
	public void canCreateOptionsWithDefaultVisitor() {
		// Act:
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder().create();

		// Assert:
		Assert.assertThat(options.getVisitor(), IsNull.notNullValue());
	}

	//region getInitialTrigger

	@Test
	public void canCreateOptionsWithInitialTrigger() {
		// Act:
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder().create();

		// Assert: 0 - trigger should be done
		Assert.assertThat(options.getInitialTrigger().isDone(), IsEqual.equalTo(true));
	}

	@Test
	public void canSetInitialDelayAsMilliseconds() throws InterruptedException {
		// Act:
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder()
				.setInitialDelay(TIME_UNIT)
				.create();

		// Assert:
		assertInitialTriggerFiresAtTime(options, TIME_UNIT);
	}

	@Test
	public void canSetInitialDelayAsTrigger() throws InterruptedException {
		// Act:
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder()
				.setTrigger(SleepFuture.create(TIME_UNIT))
				.create();

		// Assert:
		assertInitialTriggerFiresAtTime(options, TIME_UNIT);
	}

	private static void assertInitialTriggerFiresAtTime(
			final AsyncTimerOptions options,
			final int time) throws InterruptedException {
		// Assert: 0 - trigger should not be done
		Assert.assertThat(options.getInitialTrigger().isDone(), IsEqual.equalTo(false));

		// Act:
		Thread.sleep(time - TIME_HALF_UNIT);

		// Assert: time - 0.5 - trigger should not be done
		Assert.assertThat(options.getInitialTrigger().isDone(), IsEqual.equalTo(false));

		// Act:
		Thread.sleep(time + TIME_HALF_UNIT);

		// Assert: time + 0.5 - trigger should be done
		Assert.assertThat(options.getInitialTrigger().isDone(), IsEqual.equalTo(true));
	}

	@Test
	public void canSetInitialDelayAsTriggerAndMilliseconds() throws InterruptedException {
		// Act:
		final CompletableFuture<?> triggerFuture = new CompletableFuture<>();
		final AsyncTimerOptions options = new AsyncTimerOptionsBuilder()
				.setInitialDelay(TIME_HALF_UNIT)
				.setTrigger(triggerFuture)
				.create();

		// Assert: 0 - trigger should not be done
		Assert.assertThat(options.getInitialTrigger().isDone(), IsEqual.equalTo(false));

		// Act:
		Thread.sleep(2 * TIME_UNIT);
		triggerFuture.complete(null);

		// Assert: 2.0 - trigger is completed but sleep is not, so the initial trigger should not be done
		Assert.assertThat(options.getInitialTrigger().isDone(), IsEqual.equalTo(false));

		// Act:
		Thread.sleep(TIME_UNIT);

		// Act: 3.0 - the sleep is completed, so the initial trigger should be done
		Assert.assertThat(options.getInitialTrigger().isDone(), IsEqual.equalTo(true));
	}

	//region visitors

	@Test
	public void visitorIsNotifiedOfInitialDelayWhenTriggerIsNotSpecified() {
		// Act:
		final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);
		new AsyncTimerOptionsBuilder()
				.setInitialDelay(TIME_UNIT)
				.setVisitor(visitor)
				.create();

		// Assert:
		Mockito.verify(visitor, Mockito.only()).notifyDelay(TIME_UNIT);
	}

	@Test
	public void visitorIsNotifiedOfInitialDelayWhenTriggerIsSpecified() throws InterruptedException {
		// Act:
		final CompletableFuture<?> triggerFuture = new CompletableFuture<>();
		final AsyncTimerVisitor visitor = Mockito.mock(AsyncTimerVisitor.class);
		new AsyncTimerOptionsBuilder()
				.setInitialDelay(TIME_HALF_UNIT)
				.setTrigger(triggerFuture)
				.setVisitor(visitor)
				.create();

		// Assert: 0 - the visitor should not be notified (the delay happens after the trigger fires)
		Mockito.verify(visitor, Mockito.never()).notifyDelay(Mockito.anyInt());

		// Act:
		Thread.sleep(2 * TIME_UNIT);
		triggerFuture.complete(null);

		// Assert: 2.0 - the visitor should be notified after the trigger fired
		Mockito.verify(visitor, Mockito.only()).notifyDelay(TIME_HALF_UNIT);
	}

	//endregion

	//endregion
}