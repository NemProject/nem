package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.NisPeerId;

public class BroadcastablePairTest {

	@Test
	public void canCreateBroadcastablePair() {
		// Act:
		final BroadcastablePair pair = new BroadcastablePair(NisPeerId.REST_BLOCK_AT, new BlockHeight(123));

		// Assert:
		Assert.assertThat(pair.getApiId(), IsEqual.equalTo(NisPeerId.REST_BLOCK_AT));
		Assert.assertThat(pair.getEntity(), IsEqual.equalTo(new BlockHeight(123)));
	}
}
