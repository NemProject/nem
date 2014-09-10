package org.nem.core.model.primitive;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class NodeAgeTest {

	//region constructor

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedFromNegativeValue() {
		// Act:
		new NodeAge(-1);
	}

	@Test
	public void canBeCreatedFromZeroValue() {
		// Act:
		final NodeAge nodeAge = new NodeAge(0);

		// Assert:
		Assert.assertThat(nodeAge.getRaw(), IsEqual.equalTo(0L));
	}

	@Test
	public void canBeCreatedFromPositiveValue() {
		// Act:
		final NodeAge nodeAge = new NodeAge(1);

		// Assert:
		Assert.assertThat(nodeAge.getRaw(), IsEqual.equalTo(1L));
	}

	//endregion

	//region increment

	@Test
	public void nodeAgeCanBeIncremented() {
		// Arrange:
		final NodeAge nodeAge = new NodeAge(23);

		// Act:
		final NodeAge result = nodeAge.increment();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new NodeAge(24L)));
	}

	//endregion
}
