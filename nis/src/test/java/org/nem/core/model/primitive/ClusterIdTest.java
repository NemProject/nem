package org.nem.core.model.primitive;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class ClusterIdTest {

	//region constructor

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedAroundNegativeId() {
		// Act:
		new ClusterId(-1);
	}

	@Test
	public void canBeCreatedAroundZeroId() {
		// Act:
		final ClusterId id = new ClusterId(0);

		// Assert:
		Assert.assertThat(id.getValue(), IsEqual.equalTo(0));
	}

	@Test
	public void canBeCreatedAroundPositiveId() {
		// Act:
		final ClusterId id = new ClusterId(1);

		// Assert:
		Assert.assertThat(id.getValue(), IsEqual.equalTo(1));
	}

	@Test
	public void canBeCreatedFromNodeId() {
		// Act:
		final ClusterId id = new ClusterId(new NodeId(1));

		// Assert:
		Assert.assertThat(id.getValue(), IsEqual.equalTo(1));
	}

	//endregion
}
