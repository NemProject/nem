package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;

public class HashMetaDataTest {

	@Test
	public void canCreateHashMetaData() {
		// Act:
		final HashMetaData meta = new HashMetaData(new BlockHeight(10), new TimeInstant(123));

		// Assert:
		Assert.assertThat(meta.getHeight(), IsEqual.equalTo(new BlockHeight(10)));
		Assert.assertThat(meta.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
	}
}
