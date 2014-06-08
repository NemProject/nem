package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class RefrenceCounterTest {

	//region constructor

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedAroundNegativeReferenceCount() {
		// Act:
		new ReferenceCounter(-1);
	}

	@Test
	public void canBeCreatedAroundZeroReferenceCount() {
		// Act:
		final ReferenceCounter refCount = new ReferenceCounter(0);

		// Assert:
		Assert.assertThat(refCount.getRaw(), IsEqual.equalTo(0L));
	}

	@Test
	public void canBeCreatedAroundPositiveReferenceCount() {
		// Act:
		final ReferenceCounter refCount = new ReferenceCounter(1);

		// Assert:
		Assert.assertThat(refCount.getRaw(), IsEqual.equalTo(1L));
	}

	//endregion

	//region add/subtract

	@Test
	public void referenceCounterCanBeIncremented() {
		// Arrange:
		final ReferenceCounter refCounter = new ReferenceCounter(17);

		// Act:
		final ReferenceCounter result = refCounter.increment();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new ReferenceCounter(18)));
	}

	@Test
	public void referenceCounterCanBeDecrementedIfPositive() {
		// Arrange:
		final ReferenceCounter refCounter = new ReferenceCounter(17);

		// Act:
		final ReferenceCounter result = refCounter.decrement();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new ReferenceCounter(16)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void referenceCounterCannotBeDecrementedIfZero() {
		// Arrange:
		final ReferenceCounter refCounter = new ReferenceCounter(0);

		// Act:
		refCounter.decrement();
	}

	//endregion
}
