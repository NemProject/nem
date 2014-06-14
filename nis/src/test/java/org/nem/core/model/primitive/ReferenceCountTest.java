package org.nem.core.model.primitive;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class ReferenceCountTest {

	//region constructor

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedAroundNegativeReferenceCount() {
		// Act:
		new ReferenceCount(-1);
	}

	@Test
	public void canBeCreatedAroundZeroReferenceCount() {
		// Act:
		final ReferenceCount refCount = new ReferenceCount(0);

		// Assert:
		Assert.assertThat(refCount.getRaw(), IsEqual.equalTo(0L));
	}

	@Test
	public void canBeCreatedAroundPositiveReferenceCount() {
		// Act:
		final ReferenceCount refCount = new ReferenceCount(1);

		// Assert:
		Assert.assertThat(refCount.getRaw(), IsEqual.equalTo(1L));
	}

	//endregion

	//region increment/decrement

	@Test
	public void referenceCountCanBeIncremented() {
		// Arrange:
		final ReferenceCount refCount = new ReferenceCount(17);

		// Act:
		final ReferenceCount result = refCount.increment();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new ReferenceCount(18)));
	}

	@Test
	public void referenceCountCanBeDecrementedIfPositive() {
		// Arrange:
		final ReferenceCount refCount = new ReferenceCount(17);

		// Act:
		final ReferenceCount result = refCount.decrement();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new ReferenceCount(16)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void referenceCountCannotBeDecrementedIfZero() {
		// Arrange:
		final ReferenceCount refCount = new ReferenceCount(0);

		// Act:
		refCount.decrement();
	}

	//endregion
}
