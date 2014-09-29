package org.nem.core.model.primitive;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class NodeIdTest {

	//region constructor

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedAroundNegativeId() {
		// Act:
		new NodeId(-1);
	}

	@Test
	public void canBeCreatedAroundPositiveId() {
		// Act:
		final NodeId id = new NodeId(1);

		// Assert:
		Assert.assertThat(id, IsEqual.equalTo(new NodeId(1)));
	}

	//endregion
}
